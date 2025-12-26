package com.github.butvinmitmo.notificationservice

import com.github.butvinmitmo.notificationservice.entity.Notification
import java.time.Instant
import java.util.UUID

object TestEntityFactory {
    fun createNotification(
        id: UUID? = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        type: String = "NEW_INTERPRETATION",
        title: String = "Test notification",
        message: String = "Test notification message",
        isRead: Boolean = false,
        spreadId: UUID? = UUID.randomUUID(),
        interpretationId: UUID? = UUID.randomUUID(),
        createdAt: Instant? = Instant.now(),
    ): Notification =
        Notification(
            id = id,
            userId = userId,
            type = type,
            title = title,
            message = message,
            isRead = isRead,
            spreadId = spreadId,
            interpretationId = interpretationId,
            createdAt = createdAt,
        )
}
