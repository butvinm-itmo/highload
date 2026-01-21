package com.github.butvinmitmo.notificationservice.infrastructure.persistence.repository

import com.github.butvinmitmo.notificationservice.infrastructure.persistence.entity.NotificationEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface SpringDataNotificationRepository : R2dbcRepository<NotificationEntity, UUID> {
    fun findByIdAndRecipientId(
        id: UUID,
        recipientId: UUID,
    ): Mono<NotificationEntity>

    @Query(
        """
        SELECT * FROM notification
        WHERE recipient_id = :recipientId
        ORDER BY created_at DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun findByRecipientIdPaginated(
        recipientId: UUID,
        offset: Long,
        limit: Int,
    ): Flux<NotificationEntity>

    @Query(
        """
        SELECT * FROM notification
        WHERE recipient_id = :recipientId AND is_read = :isRead
        ORDER BY created_at DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun findByRecipientIdAndIsReadPaginated(
        recipientId: UUID,
        isRead: Boolean,
        offset: Long,
        limit: Int,
    ): Flux<NotificationEntity>

    fun countByRecipientId(recipientId: UUID): Mono<Long>

    fun countByRecipientIdAndIsRead(
        recipientId: UUID,
        isRead: Boolean,
    ): Mono<Long>

    fun existsByInterpretationId(interpretationId: UUID): Mono<Boolean>
}
