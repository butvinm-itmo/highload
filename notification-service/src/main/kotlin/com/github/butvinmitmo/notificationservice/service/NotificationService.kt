package com.github.butvinmitmo.notificationservice.service

import com.github.butvinmitmo.notificationservice.exception.ForbiddenException
import com.github.butvinmitmo.notificationservice.exception.NotFoundException
import com.github.butvinmitmo.notificationservice.mapper.NotificationMapper
import com.github.butvinmitmo.notificationservice.repository.NotificationRepository
import com.github.butvinmitmo.shared.dto.MarkAllReadResponse
import com.github.butvinmitmo.shared.dto.NotificationDto
import com.github.butvinmitmo.shared.dto.PageResponse
import com.github.butvinmitmo.shared.dto.UnreadCountResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val notificationMapper: NotificationMapper,
) {
    fun getNotifications(
        userId: UUID,
        page: Int,
        size: Int,
    ): Mono<PageResponse<NotificationDto>> {
        val offset = page.toLong() * size

        return notificationRepository
            .countByUserId(userId)
            .flatMap { totalElements ->
                notificationRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, offset, size)
                    .map { notificationMapper.toDto(it) }
                    .collectList()
                    .map { notifications ->
                        val totalPages = (totalElements + size - 1) / size
                        PageResponse(
                            content = notifications,
                            page = page,
                            size = size,
                            totalElements = totalElements,
                            totalPages = totalPages.toInt(),
                            isFirst = page == 0,
                            isLast = page >= totalPages - 1,
                        )
                    }
            }
    }

    fun getUnreadCount(userId: UUID): Mono<UnreadCountResponse> =
        notificationRepository
            .countUnreadByUserId(userId)
            .map { UnreadCountResponse(count = it) }

    @Transactional
    fun markAsRead(
        notificationId: UUID,
        userId: UUID,
    ): Mono<NotificationDto> =
        notificationRepository
            .findById(notificationId)
            .switchIfEmpty(Mono.error(NotFoundException("Notification not found")))
            .flatMap { notification ->
                if (notification.userId != userId) {
                    Mono.error(ForbiddenException("You can only mark your own notifications as read"))
                } else {
                    notification.isRead = true
                    notificationRepository
                        .save(notification)
                        .map { notificationMapper.toDto(it) }
                }
            }

    @Transactional
    fun markAllAsRead(userId: UUID): Mono<MarkAllReadResponse> =
        notificationRepository
            .markAllAsReadByUserId(userId)
            .map { MarkAllReadResponse(markedAsRead = it) }
}
