package com.github.butvinmitmo.shared.dto

import java.time.Instant
import java.util.UUID

/**
 * Event published when a new spread is created
 */
data class SpreadCreatedEvent(
    val eventId: UUID = UUID.randomUUID(),
    val timestamp: Instant = Instant.now(),
    val spreadId: UUID,
    val authorId: UUID,
    val authorUsername: String,
    val question: String?,
    val layoutTypeName: String,
    val cardsCount: Int,
)

/**
 * Event published when a new interpretation is added to a spread
 */
data class InterpretationCreatedEvent(
    val eventId: UUID = UUID.randomUUID(),
    val timestamp: Instant = Instant.now(),
    val interpretationId: UUID,
    val spreadId: UUID,
    val spreadAuthorId: UUID,
    val interpretationAuthorId: UUID,
    val interpretationAuthorUsername: String,
    val textPreview: String,
)
