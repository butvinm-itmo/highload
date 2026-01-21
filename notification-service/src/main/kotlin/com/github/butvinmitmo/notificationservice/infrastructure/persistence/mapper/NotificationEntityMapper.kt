package com.github.butvinmitmo.notificationservice.infrastructure.persistence.mapper

import com.github.butvinmitmo.notificationservice.domain.model.Notification
import com.github.butvinmitmo.notificationservice.infrastructure.persistence.entity.NotificationEntity
import org.springframework.stereotype.Component

@Component
class NotificationEntityMapper {
    fun toDomain(entity: NotificationEntity): Notification =
        Notification(
            id = entity.id!!,
            recipientId = entity.recipientId,
            interpretationId = entity.interpretationId,
            interpretationAuthorId = entity.interpretationAuthorId,
            spreadId = entity.spreadId,
            title = entity.title,
            message = entity.message,
            isRead = entity.isRead,
            createdAt = entity.createdAt!!,
        )

    fun toEntity(notification: Notification): NotificationEntity =
        NotificationEntity(
            id = notification.id,
            recipientId = notification.recipientId,
            interpretationId = notification.interpretationId,
            interpretationAuthorId = notification.interpretationAuthorId,
            spreadId = notification.spreadId,
            title = notification.title,
            message = notification.message,
            isRead = notification.isRead,
            createdAt = notification.createdAt,
        )
}
