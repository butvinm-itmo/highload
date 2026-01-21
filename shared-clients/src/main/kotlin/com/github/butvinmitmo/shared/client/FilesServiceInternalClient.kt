package com.github.butvinmitmo.shared.client

import com.github.butvinmitmo.shared.dto.FileUploadMetadataDto
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

/**
 * Internal Feign client for calling files-service internal endpoints.
 * Used by divination-service to verify file uploads and get download URLs.
 * This client bypasses the gateway and calls files-service directly via Eureka.
 */
@FeignClient(
    name = "files-service",
    contextId = "filesServiceInternalClient",
    url = "\${services.files-service.url:}",
    fallbackFactory = FilesServiceInternalFallbackFactory::class,
)
interface FilesServiceInternalClient {
    /**
     * Verifies that a file upload exists and marks it as completed.
     * Called by divination-service when creating an interpretation with an attachment.
     *
     * @param uploadId The ID of the upload to verify
     * @param userId The ID of the user who owns the upload (for ownership validation)
     * @return File metadata if upload exists, is valid, and belongs to the user
     */
    @PostMapping("/internal/files/{uploadId}/verify")
    fun verifyAndCompleteUpload(
        @PathVariable uploadId: UUID,
        @RequestParam userId: UUID,
    ): ResponseEntity<FileUploadMetadataDto>

    /**
     * Gets metadata for a completed file upload.
     * Called by divination-service to retrieve file information.
     *
     * @param uploadId The ID of the upload
     * @return File metadata if upload exists and is completed
     */
    @GetMapping("/internal/files/{uploadId}/metadata")
    fun getUploadMetadata(
        @PathVariable uploadId: UUID,
    ): ResponseEntity<FileUploadMetadataDto>

    /**
     * Gets a presigned download URL for a file.
     * Called by divination-service to generate download URLs for attachments.
     *
     * @param uploadId The ID of the upload
     * @return Map containing "downloadUrl" key with presigned URL
     */
    @GetMapping("/internal/files/{uploadId}/download-url")
    fun getDownloadUrl(
        @PathVariable uploadId: UUID,
    ): ResponseEntity<Map<String, String>>
}
