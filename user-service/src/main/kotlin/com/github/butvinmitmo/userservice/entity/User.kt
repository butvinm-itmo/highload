package com.github.butvinmitmo.userservice.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
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
    @Generated
    @Column(columnDefinition = "uuid", insertable = false, nullable = false)
    lateinit var id: UUID

    @Generated
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    lateinit var createdAt: Instant
}
