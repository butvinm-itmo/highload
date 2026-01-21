package com.github.butvinmitmo.filesservice.integration.controller

import com.github.butvinmitmo.filesservice.api.dto.PresignedUploadRequest
import com.github.butvinmitmo.filesservice.api.dto.PresignedUploadResponse
import com.github.butvinmitmo.filesservice.infrastructure.persistence.entity.FileUploadEntity
import com.github.butvinmitmo.filesservice.infrastructure.persistence.repository.SpringDataFileUploadRepository
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class FileUploadControllerIntegrationTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var springDataFileUploadRepository: SpringDataFileUploadRepository

    private val testUserId = UUID.randomUUID()
    private val testBucket = "test-bucket"

    @BeforeEach
    fun setup() {
        ensureBucketExists()
    }

    @AfterEach
    fun cleanup() {
        springDataFileUploadRepository.deleteAll().block()
    }

    private fun ensureBucketExists() {
        val minioClient = createMinioClient()
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(testBucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(testBucket).build())
        }
    }

    private fun createMinioClient(): MinioClient =
        MinioClient
            .builder()
            .endpoint(minio.s3URL)
            .credentials(minio.userName, minio.password)
            .build()

    @Test
    fun `requestPresignedUpload should return presigned URL for valid content type`() {
        val request =
            PresignedUploadRequest(
                fileName = "test-image.jpg",
                contentType = "image/jpeg",
            )

        webTestClient
            .post()
            .uri("/api/v0.0.1/files/presigned-upload")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(PresignedUploadResponse::class.java)
            .consumeWith { response ->
                val body = response.responseBody!!
                assert(body.uploadId != null) { "uploadId should not be null" }
                assert(body.uploadUrl.isNotEmpty()) { "uploadUrl should not be empty" }
                assert(body.uploadUrl.contains("minio")) { "uploadUrl should contain minio endpoint" }
                assert(body.expiresAt.isAfter(Instant.now())) { "expiresAt should be in the future" }
            }
    }

    @Test
    fun `requestPresignedUpload should reject invalid content type`() {
        val request =
            PresignedUploadRequest(
                fileName = "document.pdf",
                contentType = "application/pdf",
            )

        webTestClient
            .post()
            .uri("/api/v0.0.1/files/presigned-upload")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    @Test
    fun `requestPresignedUpload should return 401 without auth headers`() {
        val request =
            PresignedUploadRequest(
                fileName = "test-image.jpg",
                contentType = "image/jpeg",
            )

        webTestClient
            .post()
            .uri("/api/v0.0.1/files/presigned-upload")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `getUploadMetadata should return metadata for completed upload`() {
        // Use a fixed file path that we can create before the entity
        val content = "test image content".toByteArray()
        val fileId = UUID.randomUUID()
        val filePath = "$testUserId/$fileId/test-image.jpg"

        // Upload a file to MinIO first
        val minioClient = createMinioClient()
        minioClient.putObject(
            PutObjectArgs
                .builder()
                .bucket(testBucket)
                .`object`(filePath)
                .stream(ByteArrayInputStream(content), content.size.toLong(), -1)
                .contentType("image/jpeg")
                .build(),
        )

        // Create a completed upload in the database with the file path
        val entity =
            FileUploadEntity(
                id = null,
                userId = testUserId,
                filePath = filePath,
                originalFileName = "test-image.jpg",
                contentType = "image/jpeg",
                fileSize = content.size.toLong(),
                status = "COMPLETED",
                createdAt = null,
                expiresAt = Instant.now().plusSeconds(3600),
                completedAt = Instant.now(),
            )
        val savedEntity = springDataFileUploadRepository.save(entity).block()!!
        val uploadId = savedEntity.id!!

        webTestClient
            .get()
            .uri("/api/v0.0.1/files/$uploadId")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.uploadId")
            .isEqualTo(uploadId.toString())
            .jsonPath("$.originalFileName")
            .isEqualTo("test-image.jpg")
            .jsonPath("$.contentType")
            .isEqualTo("image/jpeg")
            .jsonPath("$.fileSize")
            .isEqualTo(content.size)
    }

    @Test
    fun `getUploadMetadata should return error for pending upload`() {
        val entity =
            FileUploadEntity(
                id = null,
                userId = testUserId,
                filePath = "temp-path",
                originalFileName = "test-image.jpg",
                contentType = "image/jpeg",
                fileSize = null,
                status = "PENDING",
                createdAt = null,
                expiresAt = Instant.now().plusSeconds(3600),
                completedAt = null,
            )
        val savedEntity = springDataFileUploadRepository.save(entity).block()!!
        val uploadId = savedEntity.id!!

        webTestClient
            .get()
            .uri("/api/v0.0.1/files/$uploadId")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    @Test
    fun `getDownloadUrl should return presigned download URL for completed upload`() {
        val content = "test image content".toByteArray()
        val fileId = UUID.randomUUID()
        val filePath = "$testUserId/$fileId/test-image.jpg"

        // Upload a file to MinIO first
        val minioClient = createMinioClient()
        minioClient.putObject(
            PutObjectArgs
                .builder()
                .bucket(testBucket)
                .`object`(filePath)
                .stream(ByteArrayInputStream(content), content.size.toLong(), -1)
                .contentType("image/jpeg")
                .build(),
        )

        val entity =
            FileUploadEntity(
                id = null,
                userId = testUserId,
                filePath = filePath,
                originalFileName = "test-image.jpg",
                contentType = "image/jpeg",
                fileSize = content.size.toLong(),
                status = "COMPLETED",
                createdAt = null,
                expiresAt = Instant.now().plusSeconds(3600),
                completedAt = Instant.now(),
            )
        val savedEntity = springDataFileUploadRepository.save(entity).block()!!
        val uploadId = savedEntity.id!!

        webTestClient
            .get()
            .uri("/api/v0.0.1/files/$uploadId/download-url")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.downloadUrl")
            .isNotEmpty
    }

    @Test
    fun `deleteUpload should delete file and record`() {
        val content = "test image content".toByteArray()
        val fileId = UUID.randomUUID()
        val filePath = "$testUserId/$fileId/test-image.jpg"

        // Upload a file to MinIO first
        val minioClient = createMinioClient()
        minioClient.putObject(
            PutObjectArgs
                .builder()
                .bucket(testBucket)
                .`object`(filePath)
                .stream(ByteArrayInputStream(content), content.size.toLong(), -1)
                .contentType("image/jpeg")
                .build(),
        )

        val entity =
            FileUploadEntity(
                id = null,
                userId = testUserId,
                filePath = filePath,
                originalFileName = "test-image.jpg",
                contentType = "image/jpeg",
                fileSize = content.size.toLong(),
                status = "COMPLETED",
                createdAt = null,
                expiresAt = Instant.now().plusSeconds(3600),
                completedAt = Instant.now(),
            )
        val savedEntity = springDataFileUploadRepository.save(entity).block()!!
        val uploadId = savedEntity.id!!

        webTestClient
            .delete()
            .uri("/api/v0.0.1/files/$uploadId")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
            .exchange()
            .expectStatus()
            .isNoContent

        // Verify record is deleted
        val deleted = springDataFileUploadRepository.findById(uploadId).block()
        assert(deleted == null) { "Upload record should be deleted" }
    }

    @Test
    fun `deleteUpload should reject upload belonging to different user`() {
        val anotherUserId = UUID.randomUUID()

        val entity =
            FileUploadEntity(
                id = null,
                userId = anotherUserId,
                filePath = "temp-path",
                originalFileName = "test-image.jpg",
                contentType = "image/jpeg",
                fileSize = 1024L,
                status = "COMPLETED",
                createdAt = null,
                expiresAt = Instant.now().plusSeconds(3600),
                completedAt = Instant.now(),
            )
        val savedEntity = springDataFileUploadRepository.save(entity).block()!!
        val uploadId = savedEntity.id!!

        webTestClient
            .delete()
            .uri("/api/v0.0.1/files/$uploadId")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
            .exchange()
            .expectStatus()
            .is5xxServerError

        // Verify record is NOT deleted
        val stillExists = springDataFileUploadRepository.findById(uploadId).block()
        assert(stillExists != null) { "Upload record should still exist" }
    }

    @Test
    fun `internal verify endpoint should complete pending upload when file exists`() {
        val content = "test image content".toByteArray()
        val fileId = UUID.randomUUID()
        val filePath = "$testUserId/$fileId/test-image.jpg"

        // Upload a file to MinIO first
        val minioClient = createMinioClient()
        minioClient.putObject(
            PutObjectArgs
                .builder()
                .bucket(testBucket)
                .`object`(filePath)
                .stream(ByteArrayInputStream(content), content.size.toLong(), -1)
                .contentType("image/jpeg")
                .build(),
        )

        val entity =
            FileUploadEntity(
                id = null,
                userId = testUserId,
                filePath = filePath,
                originalFileName = "test-image.jpg",
                contentType = "image/jpeg",
                fileSize = null,
                status = "PENDING",
                createdAt = null,
                expiresAt = Instant.now().plusSeconds(3600),
                completedAt = null,
            )
        val savedEntity = springDataFileUploadRepository.save(entity).block()!!
        val uploadId = savedEntity.id!!

        webTestClient
            .post()
            .uri("/internal/files/$uploadId/verify?userId=$testUserId")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.uploadId")
            .isEqualTo(uploadId.toString())
            .jsonPath("$.fileSize")
            .isEqualTo(content.size)

        // Verify status is now COMPLETED
        val updated = springDataFileUploadRepository.findById(uploadId).block()!!
        assert(updated.status == "COMPLETED") { "Upload status should be COMPLETED" }
        assert(updated.fileSize == content.size.toLong()) { "File size should be set" }
    }

    @Test
    fun `internal verify endpoint should reject wrong user`() {
        val wrongUserId = UUID.randomUUID()

        val entity =
            FileUploadEntity(
                id = null,
                userId = testUserId,
                filePath = "temp-path",
                originalFileName = "test-image.jpg",
                contentType = "image/jpeg",
                fileSize = null,
                status = "PENDING",
                createdAt = null,
                expiresAt = Instant.now().plusSeconds(3600),
                completedAt = null,
            )
        val savedEntity = springDataFileUploadRepository.save(entity).block()!!
        val uploadId = savedEntity.id!!

        webTestClient
            .post()
            .uri("/internal/files/$uploadId/verify?userId=$wrongUserId")
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:15-alpine")
                .withDatabaseName("tarot_db_test")
                .withUsername("test_user")
                .withPassword("test_password")

        @Container
        @JvmStatic
        val minio: MinIOContainer =
            MinIOContainer("minio/minio:latest")
                .withUserName("minioadmin")
                .withPassword("minioadmin")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
            registry.add("spring.flyway.url") { postgres.jdbcUrl }
            registry.add("spring.flyway.user") { postgres.username }
            registry.add("spring.flyway.password") { postgres.password }
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("minio.endpoint") { minio.s3URL }
            registry.add("minio.access-key") { minio.userName }
            registry.add("minio.secret-key") { minio.password }
            registry.add("minio.bucket") { "test-bucket" }
        }
    }
}
