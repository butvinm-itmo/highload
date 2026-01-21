package com.github.butvinmitmo.shared.dto

import java.time.Instant
import java.util.UUID

data class NotificationDto(
    val id: UUID,
    val recipientId: UUID,
    val spreadId: UUID,
    val interpretationAuthorId: UUID,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: Instant,
)

data class UnreadCountResponse(
    val count: Long,
)
