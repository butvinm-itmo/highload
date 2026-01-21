package com.github.butvinmitmo.filesservice.unit.service

import com.github.butvinmitmo.filesservice.application.interfaces.provider.CurrentUserProvider
import com.github.butvinmitmo.filesservice.application.interfaces.publisher.FileEventPublisher
import com.github.butvinmitmo.filesservice.application.interfaces.repository.FileUploadRepository
import com.github.butvinmitmo.filesservice.application.interfaces.storage.FileStorage
import com.github.butvinmitmo.filesservice.application.service.FileUploadService
import com.github.butvinmitmo.filesservice.config.UploadProperties
import com.github.butvinmitmo.filesservice.domain.model.FileUpload
import com.github.butvinmitmo.filesservice.domain.model.FileUploadStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class FileUploadServiceTest {
    @Mock
    private lateinit var fileUploadRepository: FileUploadRepository

    @Mock
    private lateinit var fileStorage: FileStorage

    @Mock
    private lateinit var currentUserProvider: CurrentUserProvider

    @Mock
    private lateinit var fileEventPublisher: FileEventPublisher

    private lateinit var uploadProperties: UploadProperties
    private lateinit var fileUploadService: FileUploadService

    private val testUserId = UUID.randomUUID()
    private val testUploadId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        uploadProperties =
            UploadProperties(
                expirationMinutes = 60,
                maxFileSize = 5242880L,
                allowedContentTypes = listOf("image/jpeg", "image/png", "image/gif", "image/webp"),
                cleanupIntervalMs = 300000L,
            )

        fileUploadService =
            FileUploadService(
                fileUploadRepository,
                fileStorage,
                currentUserProvider,
                uploadProperties,
                fileEventPublisher,
            )
    }

    @Test
    fun `requestUpload should create pending upload and return presigned URL`() {
        val savedUpload = createTestFileUpload(testUploadId, testUserId, FileUploadStatus.PENDING)
        val presignedUrl = "https://minio.example.com/presigned-upload-url"

        whenever(currentUserProvider.getCurrentUserId()).thenReturn(Mono.just(testUserId))
        whenever(fileUploadRepository.save(any<FileUpload>())).thenReturn(Mono.just(savedUpload))
        whenever(fileStorage.generatePresignedUploadUrl(any(), eq("image/jpeg"), eq(60)))
            .thenReturn(Mono.just(presignedUrl))

        StepVerifier
            .create(fileUploadService.requestUpload("test-image.jpg", "image/jpeg"))
            .assertNext { result ->
                assert(result.uploadId == testUploadId)
                assert(result.uploadUrl == presignedUrl)
            }.verifyComplete()

        verify(fileUploadRepository).save(
            argThat { upload: FileUpload ->
                upload.userId == testUserId &&
                    upload.contentType == "image/jpeg" &&
                    upload.status == FileUploadStatus.PENDING
            },
        )
    }

    @Test
    fun `requestUpload should reject invalid content type`() {
        StepVerifier
            .create(fileUploadService.requestUpload("document.pdf", "application/pdf"))
            .expectErrorMatches { error ->
                error is IllegalArgumentException &&
                    error.message?.contains("Content type 'application/pdf' is not allowed") == true
            }.verify()

        verify(fileUploadRepository, never()).save(any<FileUpload>())
        verify(fileStorage, never()).generatePresignedUploadUrl(any(), any(), any())
    }

    @Test
    fun `verifyAndCompleteUpload should complete upload when file exists and is valid`() {
        val pendingUpload = createTestFileUpload(testUploadId, testUserId, FileUploadStatus.PENDING)
        val fileSize = 1024L

        whenever(fileUploadRepository.findByIdAndUserId(testUploadId, testUserId))
            .thenReturn(Mono.just(pendingUpload))
        whenever(fileStorage.exists(pendingUpload.filePath)).thenReturn(Mono.just(true))
        whenever(fileStorage.getObjectSize(pendingUpload.filePath)).thenReturn(Mono.just(fileSize))
        whenever(
            fileUploadRepository.updateStatus(eq(testUploadId), eq(FileUploadStatus.COMPLETED), eq(fileSize), any()),
        ).thenReturn(Mono.empty())
        whenever(fileEventPublisher.publishCompleted(any())).thenReturn(Mono.empty())

        StepVerifier
            .create(fileUploadService.verifyAndCompleteUpload(testUploadId, testUserId))
            .assertNext { metadata ->
                assert(metadata.uploadId == testUploadId)
                assert(metadata.fileSize == fileSize)
                assert(metadata.originalFileName == "test-file.jpg")
            }.verifyComplete()
    }

    @Test
    fun `verifyAndCompleteUpload should reject upload belonging to different user`() {
        val differentUserId = UUID.randomUUID()

        whenever(fileUploadRepository.findByIdAndUserId(testUploadId, differentUserId))
            .thenReturn(Mono.empty())

        StepVerifier
            .create(fileUploadService.verifyAndCompleteUpload(testUploadId, differentUserId))
            .expectErrorMatches { error ->
                error is IllegalArgumentException &&
                    error.message?.contains("Upload not found or does not belong to user") == true
            }.verify()
    }

    @Test
    fun `verifyAndCompleteUpload should return existing metadata for already completed upload`() {
        val completedUpload =
            createTestFileUpload(testUploadId, testUserId, FileUploadStatus.COMPLETED)
                .copy(fileSize = 2048L, completedAt = Instant.now())

        whenever(fileUploadRepository.findByIdAndUserId(testUploadId, testUserId))
            .thenReturn(Mono.just(completedUpload))

        StepVerifier
            .create(fileUploadService.verifyAndCompleteUpload(testUploadId, testUserId))
            .assertNext { metadata ->
                assert(metadata.uploadId == testUploadId)
                assert(metadata.fileSize == 2048L)
            }.verifyComplete()

        verify(fileStorage, never()).exists(any())
    }

    @Test
    fun `verifyAndCompleteUpload should reject expired upload`() {
        val expiredUpload =
            createTestFileUpload(testUploadId, testUserId, FileUploadStatus.PENDING)
                .copy(expiresAt = Instant.now().minusSeconds(3600))

        whenever(fileUploadRepository.findByIdAndUserId(testUploadId, testUserId))
            .thenReturn(Mono.just(expiredUpload))

        StepVerifier
            .create(fileUploadService.verifyAndCompleteUpload(testUploadId, testUserId))
            .expectErrorMatches { error ->
                error is IllegalStateException &&
                    error.message?.contains("Upload has expired") == true
            }.verify()
    }

    @Test
    fun `verifyAndCompleteUpload should reject when file not uploaded to storage`() {
        val pendingUpload = createTestFileUpload(testUploadId, testUserId, FileUploadStatus.PENDING)

        whenever(fileUploadRepository.findByIdAndUserId(testUploadId, testUserId))
            .thenReturn(Mono.just(pendingUpload))
        whenever(fileStorage.exists(pendingUpload.filePath)).thenReturn(Mono.just(false))

        StepVerifier
            .create(fileUploadService.verifyAndCompleteUpload(testUploadId, testUserId))
            .expectErrorMatches { error ->
                error is IllegalStateException &&
                    error.message?.contains("File has not been uploaded to storage") == true
            }.verify()
    }

    @Test
    fun `verifyAndCompleteUpload should reject file exceeding max size`() {
        val pendingUpload = createTestFileUpload(testUploadId, testUserId, FileUploadStatus.PENDING)
        val oversizedFileSize = uploadProperties.maxFileSize + 1

        whenever(fileUploadRepository.findByIdAndUserId(testUploadId, testUserId))
            .thenReturn(Mono.just(pendingUpload))
        whenever(fileStorage.exists(pendingUpload.filePath)).thenReturn(Mono.just(true))
        whenever(fileStorage.getObjectSize(pendingUpload.filePath)).thenReturn(Mono.just(oversizedFileSize))
        whenever(fileStorage.delete(pendingUpload.filePath)).thenReturn(Mono.empty())

        StepVerifier
            .create(fileUploadService.verifyAndCompleteUpload(testUploadId, testUserId))
            .expectErrorMatches { error ->
                error is IllegalArgumentException &&
                    error.message?.contains("exceeds maximum allowed size") == true
            }.verify()

        verify(fileStorage).delete(pendingUpload.filePath)
    }

    @Test
    fun `getUploadMetadata should return metadata for completed upload`() {
        val completedUpload =
            createTestFileUpload(testUploadId, testUserId, FileUploadStatus.COMPLETED)
                .copy(fileSize = 1024L, completedAt = Instant.now())

        whenever(fileUploadRepository.findById(testUploadId)).thenReturn(Mono.just(completedUpload))

        StepVerifier
            .create(fileUploadService.getUploadMetadata(testUploadId))
            .assertNext { metadata ->
                assert(metadata.uploadId == testUploadId)
                assert(metadata.originalFileName == "test-file.jpg")
                assert(metadata.contentType == "image/jpeg")
            }.verifyComplete()
    }

    @Test
    fun `getUploadMetadata should reject when upload not found`() {
        whenever(fileUploadRepository.findById(testUploadId)).thenReturn(Mono.empty())

        StepVerifier
            .create(fileUploadService.getUploadMetadata(testUploadId))
            .expectErrorMatches { error ->
                error is IllegalArgumentException &&
                    error.message?.contains("Upload not found") == true
            }.verify()
    }

    @Test
    fun `getUploadMetadata should reject when upload not completed`() {
        val pendingUpload = createTestFileUpload(testUploadId, testUserId, FileUploadStatus.PENDING)

        whenever(fileUploadRepository.findById(testUploadId)).thenReturn(Mono.just(pendingUpload))

        StepVerifier
            .create(fileUploadService.getUploadMetadata(testUploadId))
            .expectErrorMatches { error ->
                error is IllegalStateException &&
                    error.message?.contains("Upload is not completed") == true
            }.verify()
    }

    @Test
    fun `getDownloadUrl should generate presigned URL for completed upload`() {
        val completedUpload =
            createTestFileUpload(testUploadId, testUserId, FileUploadStatus.COMPLETED)
                .copy(fileSize = 1024L, completedAt = Instant.now())
        val downloadUrl = "https://minio.example.com/presigned-download-url"

        whenever(fileUploadRepository.findById(testUploadId)).thenReturn(Mono.just(completedUpload))
        whenever(fileStorage.generatePresignedDownloadUrl(completedUpload.filePath, 60))
            .thenReturn(Mono.just(downloadUrl))

        StepVerifier
            .create(fileUploadService.getDownloadUrl(testUploadId))
            .assertNext { url ->
                assert(url == downloadUrl)
            }.verifyComplete()
    }

    @Test
    fun `deleteUpload should delete file from storage and database`() {
        val upload = createTestFileUpload(testUploadId, testUserId, FileUploadStatus.COMPLETED)

        whenever(currentUserProvider.getCurrentUserId()).thenReturn(Mono.just(testUserId))
        whenever(fileUploadRepository.findByIdAndUserId(testUploadId, testUserId))
            .thenReturn(Mono.just(upload))
        whenever(fileStorage.delete(upload.filePath)).thenReturn(Mono.empty())
        whenever(fileEventPublisher.publishDeleted(any())).thenReturn(Mono.empty())
        whenever(fileUploadRepository.deleteById(testUploadId)).thenReturn(Mono.empty())

        StepVerifier
            .create(fileUploadService.deleteUpload(testUploadId))
            .verifyComplete()

        verify(fileStorage).delete(upload.filePath)
        verify(fileEventPublisher).publishDeleted(upload)
        verify(fileUploadRepository).deleteById(testUploadId)
    }

    @Test
    fun `deleteUpload should reject upload belonging to different user`() {
        whenever(currentUserProvider.getCurrentUserId()).thenReturn(Mono.just(testUserId))
        whenever(fileUploadRepository.findByIdAndUserId(testUploadId, testUserId))
            .thenReturn(Mono.empty())

        StepVerifier
            .create(fileUploadService.deleteUpload(testUploadId))
            .expectErrorMatches { error ->
                error is IllegalArgumentException &&
                    error.message?.contains("Upload not found or does not belong to user") == true
            }.verify()

        verify(fileStorage, never()).delete(any())
        verify(fileUploadRepository, never()).deleteById(any())
    }

    private fun createTestFileUpload(
        id: UUID,
        userId: UUID,
        status: FileUploadStatus,
    ): FileUpload =
        FileUpload(
            id = id,
            userId = userId,
            filePath = "$userId/$id/test-file.jpg",
            originalFileName = "test-file.jpg",
            contentType = "image/jpeg",
            fileSize = null,
            status = status,
            createdAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(3600),
            completedAt = null,
        )
}
