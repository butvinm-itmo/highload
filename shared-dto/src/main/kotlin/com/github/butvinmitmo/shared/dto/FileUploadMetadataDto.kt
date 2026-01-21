package com.github.butvinmitmo.shared.dto

import java.time.Instant
import java.util.UUID

/**
 * File upload metadata returned by files-service internal API.
 * Used by divination-service via Feign client to get file information.
 */
data class FileUploadMetadataDto(
    val uploadId: UUID,
    val filePath: String,
    val originalFileName: String,
    val contentType: String,
    val fileSize: Long,
    val completedAt: Instant,
)
