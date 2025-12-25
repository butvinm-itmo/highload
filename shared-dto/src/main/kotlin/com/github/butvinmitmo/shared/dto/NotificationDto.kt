package com.github.butvinmitmo.shared.dto

import java.time.Instant
import java.util.UUID

data class NotificationDto(
    val id: UUID,
    val type: NotificationType,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: Instant,
    val referenceId: UUID,
    val referenceType: ReferenceType,
)

enum class NotificationType {
    NEW_SPREAD,
    NEW_INTERPRETATION,
}

enum class ReferenceType {
    SPREAD,
    INTERPRETATION,
}

data class UnreadCountResponse(
    val count: Long,
)

data class MarkAllReadResponse(
    val markedAsRead: Long,
)
