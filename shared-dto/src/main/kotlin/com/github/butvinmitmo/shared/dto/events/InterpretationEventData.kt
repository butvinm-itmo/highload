package com.github.butvinmitmo.shared.dto.events

import java.time.Instant
import java.util.UUID

data class InterpretationEventData(
    val id: UUID,
    val text: String,
    val authorId: UUID,
    val spreadId: UUID,
    val createdAt: Instant,
)
