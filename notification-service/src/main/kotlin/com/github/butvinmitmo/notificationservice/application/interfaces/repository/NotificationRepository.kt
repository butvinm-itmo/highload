package com.github.butvinmitmo.notificationservice.application.interfaces.repository

import com.github.butvinmitmo.notificationservice.domain.model.Notification
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface NotificationRepository {
    fun findById(id: UUID): Mono<Notification>

    fun findByIdAndRecipientId(
        id: UUID,
        recipientId: UUID,
    ): Mono<Notification>

    fun findByRecipientIdPaginated(
        recipientId: UUID,
        isRead: Boolean?,
        offset: Long,
        limit: Int,
    ): Flux<Notification>

    fun countByRecipientId(recipientId: UUID): Mono<Long>

    fun countByRecipientIdAndIsRead(
        recipientId: UUID,
        isRead: Boolean,
    ): Mono<Long>

    fun save(notification: Notification): Mono<Notification>

    fun existsByInterpretationId(interpretationId: UUID): Mono<Boolean>
}
