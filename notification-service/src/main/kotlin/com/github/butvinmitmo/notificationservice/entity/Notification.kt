package com.github.butvinmitmo.notificationservice.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("notification")
data class Notification(
    @Id
    val id: UUID? = null,
    @Column("user_id")
    val userId: UUID,
    @Column("type")
    val type: String,
    @Column("title")
    val title: String,
    @Column("message")
    val message: String,
    @Column("is_read")
    var isRead: Boolean = false,
    @Column("reference_id")
    val referenceId: UUID,
    @Column("reference_type")
    val referenceType: String,
    @Column("created_at")
    val createdAt: Instant? = null,
)
