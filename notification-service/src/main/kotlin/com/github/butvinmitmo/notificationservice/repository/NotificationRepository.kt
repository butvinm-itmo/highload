package com.github.butvinmitmo.notificationservice.repository

import com.github.butvinmitmo.notificationservice.entity.Notification
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface NotificationRepository : R2dbcRepository<Notification, UUID> {
    @Query("SELECT * FROM notification WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    fun findByUserIdOrderByCreatedAtDesc(
        userId: UUID,
        offset: Long,
        limit: Int,
    ): Flux<Notification>

    @Query("SELECT COUNT(*) FROM notification WHERE user_id = :userId")
    fun countByUserId(userId: UUID): Mono<Long>

    @Query("SELECT COUNT(*) FROM notification WHERE user_id = :userId AND is_read = false")
    fun countUnreadByUserId(userId: UUID): Mono<Long>

    @Query("UPDATE notification SET is_read = true WHERE user_id = :userId AND is_read = false")
    fun markAllAsReadByUserId(userId: UUID): Mono<Long>
}
