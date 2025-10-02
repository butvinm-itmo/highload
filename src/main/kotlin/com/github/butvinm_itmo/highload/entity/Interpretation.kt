package com.github.butvinm_itmo.highload.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "interpretation", 
    uniqueConstraints = [UniqueConstraint(columnNames = ["author_id", "spread_id"])]
)
data class Interpretation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    val id: UUID? = null,
    
    @Column(nullable = false, columnDefinition = "text")
    val text: String,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    val author: User,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spread_id", nullable = false)
    val spread: Spread
)