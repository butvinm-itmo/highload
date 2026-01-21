package com.github.butvinmitmo.filesservice.api.dto

import java.time.Instant
import java.util.UUID

data class PresignedUploadResponse(
    val uploadId: UUID,
    val uploadUrl: String,
    val expiresAt: Instant,
)
