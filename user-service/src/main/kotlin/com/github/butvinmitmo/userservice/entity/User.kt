package com.github.butvinmitmo.userservice.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "\"user\"")
class User(
    @Column(nullable = false, unique = true, length = 128)
    var username: String,
    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    var role: Role,
) {
    @Id
    @Generated
    @Column(columnDefinition = "uuid", insertable = false, nullable = false)
    lateinit var id: UUID

    @Generated
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    lateinit var createdAt: Instant
}
