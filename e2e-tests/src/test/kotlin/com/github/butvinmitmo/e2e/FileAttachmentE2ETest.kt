package com.github.butvinmitmo.e2e

import com.github.butvinmitmo.shared.dto.CreateInterpretationRequest
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.PresignedUploadRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import java.util.UUID

/**
 * E2E tests for file attachment functionality.
 *
 * Tests the API contract for file uploads and attachments.
 *
 * Note: Tests requiring direct MinIO access (upload/download via presigned URLs)
 * are disabled by default because the presigned URLs contain the internal Docker
 * hostname ('minio') which is not resolvable from the host machine.
 *
 * To run full tests including MinIO direct access:
 * 1. Add '127.0.0.1 minio' to /etc/hosts, OR
 * 2. Run E2E tests from within the Docker network
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class FileAttachmentE2ETest : BaseE2ETest() {
    companion object {
        private lateinit var mediumUserId: UUID
        private lateinit var mediumUsername: String
        private lateinit var otherMediumUserId: UUID
        private lateinit var otherMediumUsername: String
        private lateinit var oneCardLayoutId: UUID
        private lateinit var spreadId: UUID

        // Small valid PNG image bytes (1x1 pixel)
        private val TEST_PNG_BYTES =
            byteArrayOf(
                0x89.toByte(),
                0x50,
                0x4E,
                0x47,
                0x0D,
                0x0A,
                0x1A,
                0x0A,
                0x00,
                0x00,
                0x00,
                0x0D,
                0x49,
                0x48,
                0x44,
                0x52,
                0x00,
                0x00,
                0x00,
                0x01,
                0x00,
                0x00,
                0x00,
                0x01,
                0x08,
                0x02,
                0x00,
                0x00,
                0x00,
                0x90.toByte(),
                0x77,
                0x53,
                0xDE.toByte(),
                0x00,
                0x00,
                0x00,
                0x0C,
                0x49,
                0x44,
                0x41,
                0x54,
                0x08,
                0xD7.toByte(),
                0x63,
                0xF8.toByte(),
                0xCF.toByte(),
                0xC0.toByte(),
                0x00,
                0x00,
                0x00,
                0x03,
                0x00,
                0x01,
                0x00,
                0x18,
                0xDD.toByte(),
                0x8D.toByte(),
                0x00,
                0x00,
                0x00,
                0x00,
                0x49,
                0x45,
                0x4E,
                0x44,
                0xAE.toByte(),
                0x42,
                0x60,
                0x82.toByte(),
            )

        /**
         * Checks if 'minio' hostname is resolvable (e.g., via /etc/hosts entry).
         * Returns true if direct MinIO access is possible.
         */
        private fun isMinioAccessible(): Boolean =
            try {
                java.net.InetAddress.getByName("minio")
                true
            } catch (e: java.net.UnknownHostException) {
                false
            }
    }

    @BeforeAll
    fun setupTestData() {
        loginAsAdmin()

        // Create MEDIUM user for testing file uploads
        mediumUsername = "e2e_medium_user_${System.currentTimeMillis()}"
        val mediumUserResponse =
            userClient.createUser(
                currentUserId,
                currentRole,
                CreateUserRequest(
                    username = mediumUsername,
                    password = "Medium@123",
                    role = "MEDIUM",
                ),
            )
        mediumUserId = mediumUserResponse.body!!.id

        // Create another MEDIUM user to test cross-user upload rejection
        otherMediumUsername = "e2e_other_medium_${System.currentTimeMillis()}"
        val otherMediumUserResponse =
            userClient.createUser(
                currentUserId,
                currentRole,
                CreateUserRequest(
                    username = otherMediumUsername,
                    password = "Other@123",
                    role = "MEDIUM",
                ),
            )
        otherMediumUserId = otherMediumUserResponse.body!!.id

        // Get layout type
        val layoutTypes = tarotClient.getLayoutTypes(currentUserId, currentRole).body!!
        oneCardLayoutId = layoutTypes.find { it.name == "ONE_CARD" }!!.id

        // Create a spread for interpretations (as MEDIUM user)
        loginAndSetToken(mediumUsername, "Medium@123")
        val spreadResponse =
            divinationClient.createSpread(
                CreateSpreadRequest(
                    question = "E2E file attachment test spread",
                    layoutTypeId = oneCardLayoutId,
                ),
            )
        spreadId = spreadResponse.body!!.id
    }

    @AfterAll
    fun cleanup() {
        loginAsAdmin()
        // Delete test users (cascades to spreads and interpretations)
        runCatching { userClient.deleteUser(currentUserId, currentRole, mediumUserId) }
        runCatching { userClient.deleteUser(currentUserId, currentRole, otherMediumUserId) }
    }

    @Test
    @Order(1)
    fun `POST presigned-upload should return upload URL and ID`() {
        loginAndSetToken(mediumUsername, "Medium@123")

        val request =
            PresignedUploadRequest(
                fileName = "test-image.png",
                contentType = "image/png",
            )
        val response = filesClient.requestPresignedUpload(request)

        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body?.uploadId, "uploadId should not be null")
        assertNotNull(response.body?.uploadUrl, "uploadUrl should not be null")
        assertNotNull(response.body?.expiresAt, "expiresAt should not be null")

        // Verify uploadUrl format (contains MinIO reference and bucket name)
        val uploadUrl = response.body!!.uploadUrl
        assertTrue(uploadUrl.contains("interpretation-attachments"), "Upload URL should contain bucket name")
        assertTrue(
            uploadUrl.contains("X-Amz-Signature"),
            "Upload URL should be signed",
        )
    }

    @Test
    @Order(2)
    fun `POST presigned-upload with invalid content type should return 400`() {
        loginAndSetToken(mediumUsername, "Medium@123")

        val request =
            PresignedUploadRequest(
                fileName = "test.txt",
                contentType = "text/plain",
            )

        assertThrowsWithStatus(400) {
            filesClient.requestPresignedUpload(request)
        }
    }

    @Test
    @Order(3)
    fun `POST interpretation without uploadId should create interpretation without attachment`() {
        loginAndSetToken(mediumUsername, "Medium@123")

        // Create interpretation without uploadId
        val request =
            CreateInterpretationRequest(
                text = "Interpretation without file attachment - E2E test",
            )
        val response = divinationClient.createInterpretation(spreadId, request)

        assertEquals(201, response.statusCode.value())
        assertNotNull(response.body?.id)

        // Verify no attachment in spread response
        val spread = divinationClient.getSpreadById(spreadId).body!!
        val interpretation = spread.interpretations.find { it.text.contains("E2E test") }
        assertNotNull(interpretation, "Interpretation should exist")
        assertNull(interpretation!!.attachment, "Interpretation should not have attachment")
    }

    @Test
    @Order(4)
    fun `POST interpretation with non-existent uploadId should fail`() {
        loginAndSetToken(mediumUsername, "Medium@123")

        // Create a new spread (because the previous test already added interpretation to the main spreadId)
        val newSpread =
            divinationClient.createSpread(
                CreateSpreadRequest(
                    question = "Spread for non-existent uploadId test",
                    layoutTypeId = oneCardLayoutId,
                ),
            )
        val newSpreadId = newSpread.body!!.id

        val nonExistentUploadId = UUID.randomUUID()
        val request =
            CreateInterpretationRequest(
                text = "Interpretation with fake uploadId",
                uploadId = nonExistentUploadId,
            )

        // Should fail because upload doesn't exist (400 from files-service or 502 gateway error)
        assertThrowsWithStatus(400, 502) {
            divinationClient.createInterpretation(newSpreadId, request)
        }
    }

    @Test
    @Order(5)
    @Disabled("Requires 'minio' hostname to be resolvable. Add '127.0.0.1 minio' to /etc/hosts to enable.")
    fun `should upload file to MinIO via presigned URL`() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
            isMinioAccessible(),
            "MinIO not accessible - add '127.0.0.1 minio' to /etc/hosts",
        )

        loginAndSetToken(mediumUsername, "Medium@123")

        val request =
            PresignedUploadRequest(
                fileName = "test-upload.png",
                contentType = "image/png",
            )
        val presignedResponse = filesClient.requestPresignedUpload(request)
        val uploadUrl = presignedResponse.body!!.uploadUrl

        // Upload directly to MinIO
        val restTemplate = RestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.IMAGE_PNG

        val httpEntity = HttpEntity(TEST_PNG_BYTES, headers)
        val uploadResponse =
            restTemplate.exchange(
                uploadUrl,
                HttpMethod.PUT,
                httpEntity,
                String::class.java,
            )

        assertTrue(
            uploadResponse.statusCode.is2xxSuccessful,
            "File upload to MinIO should succeed, got: ${uploadResponse.statusCode}",
        )
    }

    @Test
    @Order(6)
    @Disabled("Requires 'minio' hostname to be resolvable. Add '127.0.0.1 minio' to /etc/hosts to enable.")
    fun `POST interpretation with uploadId should create interpretation with attachment`() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
            isMinioAccessible(),
            "MinIO not accessible - add '127.0.0.1 minio' to /etc/hosts",
        )

        loginAndSetToken(mediumUsername, "Medium@123")

        // First upload a file
        val uploadRequest =
            PresignedUploadRequest(
                fileName = "attachment-test.png",
                contentType = "image/png",
            )
        val presignedResponse = filesClient.requestPresignedUpload(uploadRequest)
        val uploadId = presignedResponse.body!!.uploadId

        // Upload to MinIO
        val restTemplate = RestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.IMAGE_PNG
        restTemplate.exchange(
            presignedResponse.body!!.uploadUrl,
            HttpMethod.PUT,
            HttpEntity(TEST_PNG_BYTES, headers),
            String::class.java,
        )

        // Create interpretation with uploadId
        val interpretationRequest =
            CreateInterpretationRequest(
                text = "Interpretation with attachment - full E2E test",
                uploadId = uploadId,
            )
        val response = divinationClient.createInterpretation(spreadId, interpretationRequest)

        assertEquals(201, response.statusCode.value())

        // Verify attachment in spread response
        val spread = divinationClient.getSpreadById(spreadId).body!!
        val interpretation = spread.interpretations.find { it.text.contains("full E2E test") }
        assertNotNull(interpretation?.attachment, "Interpretation should have attachment")
        assertEquals("attachment-test.png", interpretation!!.attachment!!.originalFileName)
    }

    @Test
    @Order(7)
    @Disabled("Requires 'minio' hostname to be resolvable. Add '127.0.0.1 minio' to /etc/hosts to enable.")
    fun `POST interpretation with uploadId from another user should return 403`() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
            isMinioAccessible(),
            "MinIO not accessible - add '127.0.0.1 minio' to /etc/hosts",
        )

        // Login as first MEDIUM user and request upload
        loginAndSetToken(mediumUsername, "Medium@123")

        val request =
            PresignedUploadRequest(
                fileName = "user1-image.png",
                contentType = "image/png",
            )
        val presignedResponse = filesClient.requestPresignedUpload(request)
        val user1UploadId = presignedResponse.body!!.uploadId

        // Upload the file
        val restTemplate = RestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.IMAGE_PNG
        restTemplate.exchange(
            presignedResponse.body!!.uploadUrl,
            HttpMethod.PUT,
            HttpEntity(TEST_PNG_BYTES, headers),
            String::class.java,
        )

        // Create a new spread for the other user
        loginAndSetToken(otherMediumUsername, "Other@123")
        val spreadResponse =
            divinationClient.createSpread(
                CreateSpreadRequest(
                    question = "Other user's spread for auth test",
                    layoutTypeId = oneCardLayoutId,
                ),
            )
        val otherSpreadId = spreadResponse.body!!.id

        // Try to create interpretation with user1's uploadId - should fail
        val interpretationRequest =
            CreateInterpretationRequest(
                text = "Attempting to use another user's upload",
                uploadId = user1UploadId,
            )

        assertThrowsWithStatus(403) {
            divinationClient.createInterpretation(otherSpreadId, interpretationRequest)
        }
    }

    @Test
    @Order(8)
    @Disabled("Requires 'minio' hostname to be resolvable. Add '127.0.0.1 minio' to /etc/hosts to enable.")
    fun `DELETE file upload should remove file`() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
            isMinioAccessible(),
            "MinIO not accessible - add '127.0.0.1 minio' to /etc/hosts",
        )

        loginAndSetToken(mediumUsername, "Medium@123")

        // Request a new upload
        val request =
            PresignedUploadRequest(
                fileName = "to-delete.png",
                contentType = "image/png",
            )
        val presignedResponse = filesClient.requestPresignedUpload(request)
        val deleteUploadId = presignedResponse.body!!.uploadId

        // Upload the file
        val restTemplate = RestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.IMAGE_PNG
        restTemplate.exchange(
            presignedResponse.body!!.uploadUrl,
            HttpMethod.PUT,
            HttpEntity(TEST_PNG_BYTES, headers),
            String::class.java,
        )

        // Delete the upload
        val deleteResponse = filesClient.deleteUpload(deleteUploadId)
        assertEquals(204, deleteResponse.statusCode.value())

        // Verify it's gone
        assertThrowsWithStatus(404) {
            filesClient.getUploadMetadata(deleteUploadId)
        }
    }

    @Test
    @Order(9)
    fun `DELETE pending upload should work`() {
        loginAndSetToken(mediumUsername, "Medium@123")

        // Request an upload (but don't actually upload the file)
        val request =
            PresignedUploadRequest(
                fileName = "pending-delete.png",
                contentType = "image/png",
            )
        val presignedResponse = filesClient.requestPresignedUpload(request)
        val pendingUploadId = presignedResponse.body!!.uploadId

        // Delete the pending upload (without uploading file)
        val deleteResponse = filesClient.deleteUpload(pendingUploadId)
        assertEquals(204, deleteResponse.statusCode.value())

        // Verify it's gone (returns 400 for "not found" - could be improved to 404)
        assertThrowsWithStatus(400) {
            filesClient.getUploadMetadata(pendingUploadId)
        }
    }
}
