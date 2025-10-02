package com.github.butvinm_itmo.highload.entity

import jakarta.persistence.*
import org.hibernate.annotations.GenericGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "spread")
data class Spread(
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(columnDefinition = "uuid")
    val id: UUID? = null,
    
    @Column(columnDefinition = "text")
    val question: String? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "layout_type_id", nullable = false)
    val layoutType: LayoutType,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    val author: User,
    
)