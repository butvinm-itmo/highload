package com.github.butvinmitmo.shared.dto.events

import java.time.Instant
import java.util.UUID

data class FileEventData(
    val uploadId: UUID,
    val filePath: String,
    val originalFileName: String,
    val contentType: String,
    val fileSize: Long?,
    val userId: UUID,
    val completedAt: Instant?,
)
