package com.github.butvinmitmo.userservice.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("\"user\"")
data class User(
    @Id
    val id: UUID? = null,
    @Column("username")
    val username: String,
    @Column("password_hash")
    val passwordHash: String,
    @Column("role_id")
    val roleId: UUID,
    @Column("created_at")
    val createdAt: Instant? = null,
)
