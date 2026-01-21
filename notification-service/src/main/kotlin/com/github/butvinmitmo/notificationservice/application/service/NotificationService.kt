package com.github.butvinmitmo.notificationservice.application.service

import com.github.butvinmitmo.notificationservice.application.interfaces.repository.NotificationRepository
import com.github.butvinmitmo.notificationservice.domain.model.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

data class PageResult<T>(
    val content: List<T>,
    val totalElements: Long,
)

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    fun create(
        recipientId: UUID,
        interpretationId: UUID,
        interpretationAuthorId: UUID,
        spreadId: UUID,
        title: String,
        message: String,
    ): Mono<Notification> =
        notificationRepository
            .existsByInterpretationId(interpretationId)
            .flatMap { exists ->
                if (exists) {
                    logger.debug(
                        "Notification for interpretation {} already exists, skipping",
                        interpretationId,
                    )
                    Mono.empty()
                } else {
                    val notification =
                        Notification(
                            id = null,
                            recipientId = recipientId,
                            interpretationId = interpretationId,
                            interpretationAuthorId = interpretationAuthorId,
                            spreadId = spreadId,
                            title = title,
                            message = message,
                            isRead = false,
                            createdAt = Instant.now(),
                        )
                    notificationRepository.save(notification)
                }
            }

    fun getNotificationsForUser(
        recipientId: UUID,
        isRead: Boolean?,
        page: Int,
        size: Int,
    ): Mono<PageResult<Notification>> {
        val offset = page.toLong() * size
        val countMono =
            if (isRead != null) {
                notificationRepository.countByRecipientIdAndIsRead(recipientId, isRead)
            } else {
                notificationRepository.countByRecipientId(recipientId)
            }
        return countMono.flatMap { totalElements ->
            notificationRepository
                .findByRecipientIdPaginated(recipientId, isRead, offset, size)
                .collectList()
                .map { notifications ->
                    PageResult(
                        content = notifications,
                        totalElements = totalElements,
                    )
                }
        }
    }

    fun getUnreadCountForUser(recipientId: UUID): Mono<Long> =
        notificationRepository.countByRecipientIdAndIsRead(recipientId, false)

    fun markAsRead(
        notificationId: UUID,
        recipientId: UUID,
    ): Mono<Notification> =
        notificationRepository
            .findByIdAndRecipientId(notificationId, recipientId)
            .flatMap { notification ->
                if (notification.isRead) {
                    Mono.just(notification)
                } else {
                    notificationRepository.save(notification.copy(isRead = true))
                }
            }
}
