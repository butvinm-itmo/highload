package com.github.butvinmitmo.filestorageservice.integration

import com.github.butvinmitmo.filestorageservice.service.FileStorageService
import com.github.butvinmitmo.shared.dto.FileUploadResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FileControllerIntegrationTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var fileStorageService: FileStorageService

    private val uploadedKeys = mutableListOf<String>()

    @AfterEach
    fun cleanup() {
        uploadedKeys.forEach { key ->
            try {
                fileStorageService.deleteFile(key)
            } catch (_: Exception) {
            }
        }
        uploadedKeys.clear()
    }

    @Test
    fun `upload file should return 201 with file info`() {
        val key = "test/${UUID.randomUUID()}/test-image.png"
        uploadedKeys.add(key)

        val fileContent = "fake image content".toByteArray()
        val response = uploadFile(key, fileContent, "image/png", "test-image.png")

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertNotNull(response.body)
        assertEquals(key, response.body!!.key)
        assertTrue(response.body!!.url.contains(key))
    }

    @Test
    fun `get file should return file content`() {
        val key = "test/${UUID.randomUUID()}/test-image.png"
        uploadedKeys.add(key)
        val fileContent = "fake image content".toByteArray()

        uploadFile(key, fileContent, "image/png", "test-image.png")

        val response =
            restTemplate.exchange(
                "/api/v0.0.1/files/$key",
                HttpMethod.GET,
                null,
                ByteArray::class.java,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("image/png", response.headers.contentType?.toString())
        assertEquals(String(fileContent), String(response.body!!))
    }

    @Test
    fun `get nonexistent file should return 404`() {
        val response =
            restTemplate.exchange(
                "/api/v0.0.1/files/nonexistent/file.png",
                HttpMethod.GET,
                null,
                String::class.java,
            )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `delete file should return 204`() {
        val key = "test/${UUID.randomUUID()}/test-image.png"
        val fileContent = "fake image content".toByteArray()

        uploadFile(key, fileContent, "image/png", "test-image.png")

        val response =
            restTemplate.exchange(
                "/api/v0.0.1/files?key=$key",
                HttpMethod.DELETE,
                null,
                Void::class.java,
            )

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)

        val getResponse =
            restTemplate.exchange(
                "/api/v0.0.1/files/$key",
                HttpMethod.GET,
                null,
                String::class.java,
            )
        assertEquals(HttpStatus.NOT_FOUND, getResponse.statusCode)
    }

    private fun uploadFile(
        key: String,
        content: ByteArray,
        contentType: String,
        filename: String,
    ): org.springframework.http.ResponseEntity<FileUploadResponse> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA

        val body = LinkedMultiValueMap<String, Any>()
        body.add(
            "file",
            object : ByteArrayResource(content) {
                override fun getFilename(): String = filename
            },
        )
        body.add("key", key)

        val requestEntity = HttpEntity(body, headers)

        return restTemplate.postForEntity(
            "/api/v0.0.1/files",
            requestEntity,
            FileUploadResponse::class.java,
        )
    }
}
