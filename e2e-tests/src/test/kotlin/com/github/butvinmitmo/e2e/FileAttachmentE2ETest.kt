package com.github.butvinmitmo.e2e

import com.github.butvinmitmo.e2e.config.AuthContext
import com.github.butvinmitmo.shared.dto.CreateInterpretationRequest
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.InterpretationDto
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.UUID

/**
 * E2E tests for file attachment functionality on interpretations.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class FileAttachmentE2ETest : BaseE2ETest() {
    @Value("\${services.gateway.url:http://localhost:8080}")
    private lateinit var gatewayUrl: String

    private val restTemplate = RestTemplate()

    companion object {
        private lateinit var mediumUserId: UUID
        private lateinit var mediumUsername: String
        private lateinit var layoutTypeId: UUID
        private lateinit var spreadId: UUID
        private lateinit var interpretationId: UUID
        private const val MEDIUM_PASSWORD = "Medium@123"
    }

    @BeforeAll
    fun setupTestData() {
        loginAsAdmin()

        // Create a MEDIUM user for file upload tests
        mediumUsername = "e2e_file_medium_${System.currentTimeMillis()}"
        val userResponse =
            userClient.createUser(
                currentUserId,
                currentRole,
                CreateUserRequest(
                    username = mediumUsername,
                    password = MEDIUM_PASSWORD,
                    role = "MEDIUM",
                ),
            )
        mediumUserId = userResponse.body!!.id

        // Get layout type
        val layoutTypes = tarotClient.getLayoutTypes(currentUserId, currentRole).body!!
        layoutTypeId = layoutTypes.find { it.name == "ONE_CARD" }!!.id

        // Create a spread for testing
        val spreadResponse =
            divinationClient.createSpread(
                CreateSpreadRequest(
                    question = "E2E file attachment test spread",
                    layoutTypeId = layoutTypeId,
                ),
            )
        spreadId = spreadResponse.body!!.id

        // Login as MEDIUM and create an interpretation
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)
        val interpretationResponse =
            divinationClient.createInterpretation(
                spreadId,
                CreateInterpretationRequest(text = "E2E file test interpretation"),
            )
        interpretationId = interpretationResponse.body!!.id
    }

    @AfterAll
    fun cleanup() {
        loginAsAdmin()
        // Delete MEDIUM user (cascades to spreads and interpretations)
        runCatching { userClient.deleteUser(currentUserId, currentRole, mediumUserId) }
        // Delete the spread created by admin
        runCatching { divinationClient.deleteSpread(spreadId) }
    }

    private fun createTestPngBytes(): ByteArray {
        // Minimal valid PNG (1x1 white pixel)
        return byteArrayOf(
            0x89.toByte(),
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A,
            0x0A, // PNG signature
            0x00,
            0x00,
            0x00,
            0x0D, // IHDR chunk length
            0x49,
            0x48,
            0x44,
            0x52, // IHDR
            0x00,
            0x00,
            0x00,
            0x01, // width: 1
            0x00,
            0x00,
            0x00,
            0x01, // height: 1
            0x08,
            0x02, // bit depth: 8, color type: RGB
            0x00,
            0x00,
            0x00, // compression, filter, interlace
            0x90.toByte(),
            0x77,
            0x53,
            0xDE.toByte(), // IHDR CRC
            0x00,
            0x00,
            0x00,
            0x0C, // IDAT chunk length
            0x49,
            0x44,
            0x41,
            0x54, // IDAT
            0x08,
            0xD7.toByte(),
            0x63,
            0xF8.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0x00,
            0x05,
            0xFE.toByte(),
            0x02,
            0xFE.toByte(), // compressed data
            0xA3.toByte(),
            0x6C,
            0xC4.toByte(),
            0x6B, // IDAT CRC
            0x00,
            0x00,
            0x00,
            0x00, // IEND chunk length
            0x49,
            0x45,
            0x4E,
            0x44, // IEND
            0xAE.toByte(),
            0x42,
            0x60,
            0x82.toByte(), // IEND CRC
        )
    }

    private fun uploadFile(
        spreadId: UUID,
        interpretationId: UUID,
        fileBytes: ByteArray,
        filename: String,
    ): InterpretationDto {
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.MULTIPART_FORM_DATA
                AuthContext.getToken()?.let { setBearerAuth(it) }
            }

        val fileResource =
            object : ByteArrayResource(fileBytes) {
                override fun getFilename(): String = filename
            }

        val body =
            LinkedMultiValueMap<String, Any>().apply {
                add("file", fileResource)
            }

        val request = HttpEntity(body, headers)
        val url = "$gatewayUrl/api/v0.0.1/spreads/$spreadId/interpretations/$interpretationId/file"

        val response =
            restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                InterpretationDto::class.java,
            )

        return response.body!!
    }

    private fun deleteFile(
        spreadId: UUID,
        interpretationId: UUID,
    ): InterpretationDto {
        val headers =
            HttpHeaders().apply {
                AuthContext.getToken()?.let { setBearerAuth(it) }
            }

        val request = HttpEntity<Void>(headers)
        val url = "$gatewayUrl/api/v0.0.1/spreads/$spreadId/interpretations/$interpretationId/file"

        val response =
            restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                request,
                InterpretationDto::class.java,
            )

        return response.body!!
    }

    @Test
    @Order(1)
    fun `upload PNG file should succeed and return fileUrl`() {
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)

        val pngBytes = createTestPngBytes()
        val result = uploadFile(spreadId, interpretationId, pngBytes, "test-image.png")

        assertNotNull(result.fileUrl, "Response should contain fileUrl after upload")
        assertTrue(result.fileUrl!!.contains("/api/v0.0.1/files/"), "fileUrl should point to file-storage-service")
        assertEquals(interpretationId, result.id)
    }

    @Test
    @Order(2)
    fun `GET interpretation should include fileUrl after upload`() {
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)

        val interpretation = divinationClient.getInterpretation(spreadId, interpretationId).body!!

        assertNotNull(interpretation.fileUrl, "Interpretation should have fileUrl")
    }

    @Test
    @Order(3)
    fun `download file should succeed`() {
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)

        val interpretation = divinationClient.getInterpretation(spreadId, interpretationId).body!!
        val fileUrl = interpretation.fileUrl!!

        // Download the file using the fileUrl
        val headers =
            HttpHeaders().apply {
                AuthContext.getToken()?.let { setBearerAuth(it) }
            }

        val downloadUrl = if (fileUrl.startsWith("http")) fileUrl else "$gatewayUrl$fileUrl"
        val request = HttpEntity<Void>(headers)
        val response =
            restTemplate.exchange(
                downloadUrl,
                HttpMethod.GET,
                request,
                ByteArray::class.java,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body, "File content should not be null")
        assertTrue(response.body!!.isNotEmpty(), "File content should not be empty")
    }

    @Test
    @Order(4)
    fun `upload duplicate file should return 409`() {
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)

        val pngBytes = createTestPngBytes()

        try {
            uploadFile(spreadId, interpretationId, pngBytes, "duplicate.png")
            throw AssertionError("Expected 409 Conflict but request succeeded")
        } catch (e: HttpClientErrorException) {
            assertEquals(HttpStatus.CONFLICT, e.statusCode, "Should return 409 when file already exists")
        }
    }

    @Test
    @Order(5)
    fun `delete file should succeed`() {
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)

        val result = deleteFile(spreadId, interpretationId)

        assertNull(result.fileUrl, "fileUrl should be null after deletion")
    }

    @Test
    @Order(6)
    fun `GET interpretation should not include fileUrl after deletion`() {
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)

        val interpretation = divinationClient.getInterpretation(spreadId, interpretationId).body!!

        assertNull(interpretation.fileUrl, "Interpretation should not have fileUrl after deletion")
    }

    @Test
    @Order(7)
    fun `upload JPG file should succeed`() {
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)

        // Minimal JPEG bytes (not a real valid JPEG but has correct header for validation)
        val jpegBytes =
            byteArrayOf(
                0xFF.toByte(),
                0xD8.toByte(), // SOI marker
                0xFF.toByte(),
                0xE0.toByte(), // APP0 marker
                0x00,
                0x10, // length
                0x4A,
                0x46,
                0x49,
                0x46,
                0x00, // JFIF\0
                0x01,
                0x01, // version
                0x00, // units
                0x00,
                0x01, // X density
                0x00,
                0x01, // Y density
                0x00,
                0x00, // thumbnail
                0xFF.toByte(),
                0xD9.toByte(), // EOI marker
            )

        val result = uploadFile(spreadId, interpretationId, jpegBytes, "test-image.jpg")

        assertNotNull(result.fileUrl, "Response should contain fileUrl after JPEG upload")

        // Cleanup - delete the file for next tests
        deleteFile(spreadId, interpretationId)
    }

    @Test
    @Order(8)
    fun `upload invalid file type should return 400`() {
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)

        val textBytes = "This is a text file".toByteArray()

        try {
            uploadFile(spreadId, interpretationId, textBytes, "test.txt")
            throw AssertionError("Expected 400 Bad Request but request succeeded")
        } catch (e: HttpClientErrorException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode, "Should return 400 for invalid file type")
        }
    }

    @Test
    @Order(9)
    fun `upload oversized file should return 400`() {
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)

        // Create a file larger than 2MB
        val oversizedBytes = ByteArray(2 * 1024 * 1024 + 1) // 2MB + 1 byte

        try {
            uploadFile(spreadId, interpretationId, oversizedBytes, "large.png")
            throw AssertionError("Expected 400 Bad Request but request succeeded")
        } catch (e: HttpClientErrorException) {
            assertTrue(
                e.statusCode == HttpStatus.BAD_REQUEST || e.statusCode == HttpStatus.PAYLOAD_TOO_LARGE,
                "Should return 400 or 413 for oversized file",
            )
        }
    }

    @Test
    @Order(10)
    fun `upload file by non-author should return 403`() {
        // Create another MEDIUM user
        loginAsAdmin()
        val otherUsername = "e2e_file_other_${System.currentTimeMillis()}"
        val otherUserResponse =
            userClient.createUser(
                currentUserId,
                currentRole,
                CreateUserRequest(
                    username = otherUsername,
                    password = "Other@123",
                    role = "MEDIUM",
                ),
            )
        val otherUserId = otherUserResponse.body!!.id

        try {
            // Login as other user and try to upload to the original user's interpretation
            loginAndSetToken(otherUsername, "Other@123")

            val pngBytes = createTestPngBytes()

            try {
                uploadFile(spreadId, interpretationId, pngBytes, "unauthorized.png")
                throw AssertionError("Expected 403 Forbidden but request succeeded")
            } catch (e: HttpClientErrorException) {
                assertEquals(HttpStatus.FORBIDDEN, e.statusCode, "Should return 403 for non-author upload")
            }
        } finally {
            // Cleanup
            loginAsAdmin()
            userClient.deleteUser(currentUserId, currentRole, otherUserId)
        }
    }

    @Test
    @Order(11)
    fun `delete interpretation should cascade delete file`() {
        loginAsAdmin()

        // Create a new spread and interpretation with a file
        val newSpreadResponse =
            divinationClient.createSpread(
                CreateSpreadRequest(
                    question = "E2E cascade delete test",
                    layoutTypeId = layoutTypeId,
                ),
            )
        val newSpreadId = newSpreadResponse.body!!.id

        val newInterpretationResponse =
            divinationClient.createInterpretation(
                newSpreadId,
                CreateInterpretationRequest(text = "Cascade delete test interpretation"),
            )
        val newInterpretationId = newInterpretationResponse.body!!.id

        // Upload a file
        val pngBytes = createTestPngBytes()
        val uploadResult = uploadFile(newSpreadId, newInterpretationId, pngBytes, "cascade-test.png")
        val fileUrl = uploadResult.fileUrl!!

        // Delete the interpretation
        divinationClient.deleteInterpretation(newSpreadId, newInterpretationId)

        // Verify file is no longer accessible
        val headers =
            HttpHeaders().apply {
                AuthContext.getToken()?.let { setBearerAuth(it) }
            }
        val downloadUrl = if (fileUrl.startsWith("http")) fileUrl else "$gatewayUrl$fileUrl"
        val request = HttpEntity<Void>(headers)

        try {
            restTemplate.exchange(
                downloadUrl,
                HttpMethod.GET,
                request,
                ByteArray::class.java,
            )
            throw AssertionError("Expected 404 Not Found but request succeeded")
        } catch (e: HttpClientErrorException) {
            assertEquals(HttpStatus.NOT_FOUND, e.statusCode, "File should be deleted when interpretation is deleted")
        }

        // Cleanup - delete the spread
        divinationClient.deleteSpread(newSpreadId)
    }
}
