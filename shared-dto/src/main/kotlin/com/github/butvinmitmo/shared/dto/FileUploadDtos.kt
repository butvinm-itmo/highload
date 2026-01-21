package com.github.butvinmitmo.shared.dto

import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

/**
 * Request to generate a presigned URL for file upload.
 */
data class PresignedUploadRequest(
    @field:NotBlank(message = "File name is required")
    val fileName: String,
    @field:NotBlank(message = "Content type is required")
    val contentType: String,
)

/**
 * Response containing presigned upload URL and metadata.
 */
data class PresignedUploadResponse(
    val uploadId: UUID,
    val uploadUrl: String,
    val expiresAt: Instant,
)

/**
 * Response containing presigned download URL.
 */
data class DownloadUrlResponse(
    val downloadUrl: String,
)

/**
 * Response containing file upload metadata.
 */
data class FileMetadataResponse(
    val uploadId: UUID,
    val originalFileName: String,
    val contentType: String,
    val fileSize: Long,
    val completedAt: Instant,
)
