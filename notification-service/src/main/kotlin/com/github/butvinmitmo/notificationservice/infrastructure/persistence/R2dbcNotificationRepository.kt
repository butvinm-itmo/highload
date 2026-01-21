package com.github.butvinmitmo.notificationservice.infrastructure.persistence

import com.github.butvinmitmo.notificationservice.application.interfaces.repository.NotificationRepository
import com.github.butvinmitmo.notificationservice.domain.model.Notification
import com.github.butvinmitmo.notificationservice.infrastructure.persistence.mapper.NotificationEntityMapper
import com.github.butvinmitmo.notificationservice.infrastructure.persistence.repository.SpringDataNotificationRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class R2dbcNotificationRepository(
    private val springDataNotificationRepository: SpringDataNotificationRepository,
    private val notificationEntityMapper: NotificationEntityMapper,
) : NotificationRepository {
    override fun findById(id: UUID): Mono<Notification> =
        springDataNotificationRepository.findById(id).map { notificationEntityMapper.toDomain(it) }

    override fun findByIdAndRecipientId(
        id: UUID,
        recipientId: UUID,
    ): Mono<Notification> =
        springDataNotificationRepository
            .findByIdAndRecipientId(id, recipientId)
            .map { notificationEntityMapper.toDomain(it) }

    override fun findByRecipientIdPaginated(
        recipientId: UUID,
        isRead: Boolean?,
        offset: Long,
        limit: Int,
    ): Flux<Notification> =
        if (isRead != null) {
            springDataNotificationRepository
                .findByRecipientIdAndIsReadPaginated(recipientId, isRead, offset, limit)
                .map { notificationEntityMapper.toDomain(it) }
        } else {
            springDataNotificationRepository
                .findByRecipientIdPaginated(recipientId, offset, limit)
                .map { notificationEntityMapper.toDomain(it) }
        }

    override fun countByRecipientId(recipientId: UUID): Mono<Long> =
        springDataNotificationRepository.countByRecipientId(recipientId)

    override fun countByRecipientIdAndIsRead(
        recipientId: UUID,
        isRead: Boolean,
    ): Mono<Long> = springDataNotificationRepository.countByRecipientIdAndIsRead(recipientId, isRead)

    override fun save(notification: Notification): Mono<Notification> {
        val entity = notificationEntityMapper.toEntity(notification)
        return springDataNotificationRepository
            .save(entity)
            .flatMap { savedEntity ->
                // Re-fetch to get database-generated fields (id, created_at)
                springDataNotificationRepository
                    .findById(savedEntity.id!!)
                    .map { notificationEntityMapper.toDomain(it) }
            }
    }

    override fun existsByInterpretationId(interpretationId: UUID): Mono<Boolean> =
        springDataNotificationRepository.existsByInterpretationId(interpretationId)
}
