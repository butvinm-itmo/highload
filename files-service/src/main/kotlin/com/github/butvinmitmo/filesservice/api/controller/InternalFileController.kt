package com.github.butvinmitmo.filesservice.api.controller

import com.github.butvinmitmo.filesservice.application.service.FileUploadService
import com.github.butvinmitmo.shared.dto.FileUploadMetadataDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * Internal API controller for service-to-service communication.
 * These endpoints are not exposed through the gateway and are only accessible
 * via Eureka service discovery for internal operations.
 */
@RestController
@RequestMapping("/internal/files")
class InternalFileController(
    private val fileUploadService: FileUploadService,
) {
    /**
     * Verifies that a file upload exists and is completed.
     * Called by divination-service when creating an interpretation with an attachment.
     *
     * @param uploadId The ID of the upload to verify
     * @param userId The ID of the user who owns the upload (for ownership validation)
     * @return File metadata if upload exists, is completed, and belongs to the user
     */
    @PostMapping("/{uploadId}/verify")
    fun verifyAndCompleteUpload(
        @PathVariable uploadId: UUID,
        @RequestParam userId: UUID,
    ): Mono<ResponseEntity<FileUploadMetadataDto>> =
        fileUploadService.verifyAndCompleteUpload(uploadId, userId).map { metadata ->
            ResponseEntity.ok(
                FileUploadMetadataDto(
                    uploadId = metadata.uploadId,
                    filePath = metadata.filePath,
                    originalFileName = metadata.originalFileName,
                    contentType = metadata.contentType,
                    fileSize = metadata.fileSize,
                    completedAt = metadata.completedAt,
                ),
            )
        }

    /**
     * Gets metadata for a completed file upload.
     * Called by divination-service to retrieve file information.
     *
     * @param uploadId The ID of the upload
     * @return File metadata if upload exists and is completed
     */
    @GetMapping("/{uploadId}/metadata")
    fun getUploadMetadata(
        @PathVariable uploadId: UUID,
    ): Mono<ResponseEntity<FileUploadMetadataDto>> =
        fileUploadService.getUploadMetadata(uploadId).map { metadata ->
            ResponseEntity.ok(
                FileUploadMetadataDto(
                    uploadId = metadata.uploadId,
                    filePath = metadata.filePath,
                    originalFileName = metadata.originalFileName,
                    contentType = metadata.contentType,
                    fileSize = metadata.fileSize,
                    completedAt = metadata.completedAt,
                ),
            )
        }

    /**
     * Gets a presigned download URL for a file.
     * Called by divination-service to generate download URLs for attachments.
     *
     * @param uploadId The ID of the upload
     * @return Presigned download URL
     */
    @GetMapping("/{uploadId}/download-url")
    fun getDownloadUrl(
        @PathVariable uploadId: UUID,
    ): Mono<ResponseEntity<Map<String, String>>> =
        fileUploadService.getDownloadUrl(uploadId).map { url ->
            ResponseEntity.ok(mapOf("downloadUrl" to url))
        }
}
