package com.github.butvinmitmo.notificationservice.domain.model

import java.time.Instant
import java.util.UUID

data class Notification(
    val id: UUID?,
    val recipientId: UUID,
    val interpretationId: UUID,
    val interpretationAuthorId: UUID,
    val spreadId: UUID,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: Instant?,
)
