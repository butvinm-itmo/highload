package com.github.butvinmitmo.shared.dto

import java.util.UUID

/**
 * DTO for interpretation attachment information returned in API responses.
 * Contains file metadata and a presigned download URL.
 */
data class InterpretationAttachmentDto(
    val id: UUID,
    val originalFileName: String,
    val contentType: String,
    val fileSize: Long,
    val downloadUrl: String,
)
