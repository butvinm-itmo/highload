package com.github.butvinmitmo.notificationservice.api.mapper

import com.github.butvinmitmo.notificationservice.domain.model.Notification
import com.github.butvinmitmo.shared.dto.NotificationDto
import org.springframework.stereotype.Component

@Component
class NotificationDtoMapper {
    fun toDto(notification: Notification): NotificationDto =
        NotificationDto(
            id = notification.id!!,
            recipientId = notification.recipientId,
            spreadId = notification.spreadId,
            interpretationAuthorId = notification.interpretationAuthorId,
            title = notification.title,
            message = notification.message,
            isRead = notification.isRead,
            createdAt = notification.createdAt!!,
        )
}
