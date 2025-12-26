package com.github.butvinmitmo.notificationservice.mapper

import com.github.butvinmitmo.notificationservice.entity.Notification
import com.github.butvinmitmo.shared.dto.NotificationDto
import com.github.butvinmitmo.shared.dto.NotificationType
import org.springframework.stereotype.Component

@Component
class NotificationMapper {
    fun toDto(entity: Notification): NotificationDto =
        NotificationDto(
            id = entity.id!!,
            type = NotificationType.valueOf(entity.type),
            title = entity.title,
            message = entity.message,
            isRead = entity.isRead,
            createdAt = entity.createdAt!!,
            spreadId = entity.spreadId,
            interpretationId = entity.interpretationId,
        )
}
