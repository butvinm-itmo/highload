package com.github.butvinmitmo.highload.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "\"user\"")
class User(
    @Column(nullable = false, unique = true, length = 128)
    var username: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    lateinit var id: UUID

    @Generated
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    lateinit var createdAt: Instant
}
