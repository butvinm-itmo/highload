package com.github.butvinmitmo.divinationservice.domain.model

import java.time.Instant
import java.util.UUID

data class Interpretation(
    val id: UUID?,
    val text: String,
    val authorId: UUID,
    val spreadId: UUID,
    val createdAt: Instant?,
)
