package com.github.butvinmitmo.divinationservice.domain.model

import java.time.Instant
import java.util.UUID

data class InterpretationAttachment(
    val id: UUID?,
    val interpretationId: UUID,
    val fileUploadId: UUID,
    val originalFileName: String,
    val contentType: String,
    val fileSize: Long,
    val createdAt: Instant?,
)
