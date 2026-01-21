package com.github.butvinmitmo.filesservice.api.dto

import java.time.Instant
import java.util.UUID

/**
 * File upload metadata response for public API.
 */
data class FileUploadMetadataResponse(
    val uploadId: UUID,
    val originalFileName: String,
    val contentType: String,
    val fileSize: Long,
    val completedAt: Instant,
)
