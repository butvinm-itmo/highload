package com.github.butvinmitmo.filesservice.application.service

import com.github.butvinmitmo.filesservice.application.interfaces.provider.CurrentUserProvider
import com.github.butvinmitmo.filesservice.application.interfaces.repository.FileUploadRepository
import com.github.butvinmitmo.filesservice.application.interfaces.storage.FileStorage
import com.github.butvinmitmo.filesservice.config.UploadProperties
import com.github.butvinmitmo.filesservice.domain.model.FileUpload
import com.github.butvinmitmo.filesservice.domain.model.FileUploadStatus
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

data class PresignedUploadResult(
    val uploadId: UUID,
    val uploadUrl: String,
    val expiresAt: Instant,
)

data class FileUploadMetadata(
    val uploadId: UUID,
    val filePath: String,
    val originalFileName: String,
    val contentType: String,
    val fileSize: Long,
    val completedAt: Instant,
)

@Service
class FileUploadService(
    private val fileUploadRepository: FileUploadRepository,
    private val fileStorage: FileStorage,
    private val currentUserProvider: CurrentUserProvider,
    private val uploadProperties: UploadProperties,
) {
    private val logger = LoggerFactory.getLogger(FileUploadService::class.java)

    fun requestUpload(
        fileName: String,
        contentType: String,
    ): Mono<PresignedUploadResult> {
        if (contentType !in uploadProperties.allowedContentTypes) {
            return Mono.error(
                IllegalArgumentException(
                    "Content type '$contentType' is not allowed. Allowed types: ${uploadProperties.allowedContentTypes}",
                ),
            )
        }

        return currentUserProvider.getCurrentUserId().flatMap { userId ->
            val expiresAt = Instant.now().plusSeconds(uploadProperties.expirationMinutes.toLong() * 60)
            val filePath = "$userId/${UUID.randomUUID()}/${sanitizeFileName(fileName)}"

            val fileUpload =
                FileUpload(
                    id = null,
                    userId = userId,
                    filePath = filePath,
                    originalFileName = fileName,
                    contentType = contentType,
                    fileSize = null,
                    status = FileUploadStatus.PENDING,
                    createdAt = null,
                    expiresAt = expiresAt,
                    completedAt = null,
                )

            fileUploadRepository.save(fileUpload).flatMap { savedUpload ->
                fileStorage
                    .generatePresignedUploadUrl(filePath, contentType, uploadProperties.expirationMinutes)
                    .map { uploadUrl ->
                        PresignedUploadResult(
                            uploadId = savedUpload.id!!,
                            uploadUrl = uploadUrl,
                            expiresAt = expiresAt,
                        )
                    }
            }
        }
    }

    fun verifyAndCompleteUpload(
        uploadId: UUID,
        userId: UUID,
    ): Mono<FileUploadMetadata> =
        fileUploadRepository
            .findByIdAndUserId(uploadId, userId)
            .switchIfEmpty(
                Mono.error(IllegalArgumentException("Upload not found or does not belong to user")),
            ).flatMap { upload ->
                if (upload.status != FileUploadStatus.PENDING) {
                    if (upload.status == FileUploadStatus.COMPLETED) {
                        return@flatMap Mono.just(
                            FileUploadMetadata(
                                uploadId = upload.id!!,
                                filePath = upload.filePath,
                                originalFileName = upload.originalFileName,
                                contentType = upload.contentType,
                                fileSize = upload.fileSize!!,
                                completedAt = upload.completedAt!!,
                            ),
                        )
                    }
                    return@flatMap Mono.error<FileUploadMetadata>(
                        IllegalStateException("Upload is not in PENDING status"),
                    )
                }

                if (upload.expiresAt.isBefore(Instant.now())) {
                    return@flatMap Mono.error<FileUploadMetadata>(
                        IllegalStateException("Upload has expired"),
                    )
                }

                fileStorage.exists(upload.filePath).flatMap { exists ->
                    if (!exists) {
                        return@flatMap Mono.error<FileUploadMetadata>(
                            IllegalStateException("File has not been uploaded to storage"),
                        )
                    }

                    fileStorage.getObjectSize(upload.filePath).flatMap { fileSize ->
                        if (fileSize > uploadProperties.maxFileSize) {
                            fileStorage.delete(upload.filePath).then(
                                Mono.error<FileUploadMetadata>(
                                    IllegalArgumentException(
                                        "File size ($fileSize bytes) exceeds maximum allowed size " +
                                            "(${uploadProperties.maxFileSize} bytes)",
                                    ),
                                ),
                            )
                        } else {
                            val completedAt = Instant.now()
                            fileUploadRepository
                                .updateStatus(uploadId, FileUploadStatus.COMPLETED, fileSize, completedAt)
                                .then(
                                    Mono.just(
                                        FileUploadMetadata(
                                            uploadId = upload.id!!,
                                            filePath = upload.filePath,
                                            originalFileName = upload.originalFileName,
                                            contentType = upload.contentType,
                                            fileSize = fileSize,
                                            completedAt = completedAt,
                                        ),
                                    ),
                                )
                        }
                    }
                }
            }

    fun getUploadMetadata(uploadId: UUID): Mono<FileUploadMetadata> =
        fileUploadRepository
            .findById(uploadId)
            .switchIfEmpty(
                Mono.error(IllegalArgumentException("Upload not found")),
            ).flatMap { upload ->
                if (upload.status != FileUploadStatus.COMPLETED) {
                    return@flatMap Mono.error<FileUploadMetadata>(
                        IllegalStateException("Upload is not completed"),
                    )
                }
                Mono.just(
                    FileUploadMetadata(
                        uploadId = upload.id!!,
                        filePath = upload.filePath,
                        originalFileName = upload.originalFileName,
                        contentType = upload.contentType,
                        fileSize = upload.fileSize!!,
                        completedAt = upload.completedAt!!,
                    ),
                )
            }

    fun getDownloadUrl(uploadId: UUID): Mono<String> =
        fileUploadRepository
            .findById(uploadId)
            .switchIfEmpty(
                Mono.error(IllegalArgumentException("Upload not found")),
            ).flatMap { upload ->
                if (upload.status != FileUploadStatus.COMPLETED) {
                    return@flatMap Mono.error<String>(
                        IllegalStateException("Upload is not completed"),
                    )
                }
                fileStorage.generatePresignedDownloadUrl(upload.filePath, uploadProperties.expirationMinutes)
            }

    fun deleteUpload(uploadId: UUID): Mono<Void> =
        currentUserProvider.getCurrentUserId().flatMap { userId ->
            fileUploadRepository
                .findByIdAndUserId(uploadId, userId)
                .switchIfEmpty(
                    Mono.error(IllegalArgumentException("Upload not found or does not belong to user")),
                ).flatMap { upload ->
                    fileStorage
                        .delete(upload.filePath)
                        .onErrorResume { e ->
                            logger.warn("Failed to delete file from storage: ${upload.filePath}", e)
                            Mono.empty()
                        }.then(fileUploadRepository.deleteById(uploadId))
                }
        }

    @Scheduled(fixedDelayString = "\${upload.cleanup-interval-ms:300000}")
    fun cleanupExpiredUploads() {
        val now = Instant.now()
        logger.info("Starting cleanup of expired uploads")

        fileUploadRepository
            .findExpiredPending(now)
            .flatMap { upload ->
                logger.info("Cleaning up expired upload: ${upload.id}, path: ${upload.filePath}")
                fileStorage
                    .delete(upload.filePath)
                    .onErrorResume { e ->
                        logger.warn("Failed to delete expired file from storage: ${upload.filePath}", e)
                        Mono.empty()
                    }.then(fileUploadRepository.deleteById(upload.id!!))
            }.doOnComplete {
                logger.info("Completed cleanup of expired uploads")
            }.subscribe()
    }

    private fun sanitizeFileName(fileName: String): String =
        fileName
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(200)
}
