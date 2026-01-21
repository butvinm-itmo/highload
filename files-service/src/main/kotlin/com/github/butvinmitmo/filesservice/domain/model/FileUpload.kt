package com.github.butvinmitmo.filesservice.domain.model

import java.time.Instant
import java.util.UUID

data class FileUpload(
    val id: UUID?,
    val userId: UUID,
    val filePath: String,
    val originalFileName: String,
    val contentType: String,
    val fileSize: Long?,
    val status: FileUploadStatus,
    val createdAt: Instant?,
    val expiresAt: Instant,
    val completedAt: Instant?,
)
