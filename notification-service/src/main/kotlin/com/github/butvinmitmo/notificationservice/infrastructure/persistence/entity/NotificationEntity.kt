package com.github.butvinmitmo.notificationservice.infrastructure.persistence.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("notification")
data class NotificationEntity(
    @Id
    val id: UUID? = null,
    @Column("recipient_id")
    val recipientId: UUID,
    @Column("interpretation_id")
    val interpretationId: UUID,
    @Column("interpretation_author_id")
    val interpretationAuthorId: UUID,
    @Column("spread_id")
    val spreadId: UUID,
    @Column("title")
    val title: String,
    @Column("message")
    val message: String,
    @Column("is_read")
    val isRead: Boolean,
    @Column("created_at")
    val createdAt: Instant? = null,
)
