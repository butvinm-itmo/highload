package com.github.butvinmitmo.shared.dto.events

import java.time.Instant
import java.util.UUID

data class SpreadEventData(
    val id: UUID,
    val question: String?,
    val layoutTypeId: UUID,
    val authorId: UUID,
    val createdAt: Instant,
)
