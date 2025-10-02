package com.github.butvinm_itmo.highload.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    val id: UUID? = null,
    
    @Column(nullable = false, unique = true, length = 128)
    val username: String,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)