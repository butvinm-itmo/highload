package com.github.butvinmitmo.divinationservice.application.interfaces.provider

import com.github.butvinmitmo.shared.dto.FileUploadMetadataDto
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * Provider interface for file operations via files-service.
 * Used to verify uploads, get metadata, and generate download URLs.
 */
interface FileProvider {
    /**
     * Verifies that an upload exists, belongs to the user, and marks it as completed.
     *
     * @param uploadId The ID of the upload to verify
     * @param userId The ID of the user who should own the upload
     * @return File metadata if verification succeeds
     */
    fun verifyAndCompleteUpload(
        uploadId: UUID,
        userId: UUID,
    ): Mono<FileUploadMetadataDto>

    /**
     * Gets metadata for a completed file upload.
     *
     * @param uploadId The ID of the upload
     * @return File metadata
     */
    fun getUploadMetadata(uploadId: UUID): Mono<FileUploadMetadataDto>

    /**
     * Gets a presigned download URL for a file.
     *
     * @param uploadId The ID of the upload
     * @return Presigned download URL
     */
    fun getDownloadUrl(uploadId: UUID): Mono<String>
}
