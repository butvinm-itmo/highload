package com.github.butvinmitmo.divinationservice.domain.model

import java.time.Instant
import java.util.UUID

data class Spread(
    val id: UUID?,
    val question: String?,
    val layoutTypeId: UUID,
    val authorId: UUID,
    val createdAt: Instant?,
)
