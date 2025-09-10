package com.itmo.tarot.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "spreads")
data class Spread(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false, length = 1000)
    val question: String,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "layout_type", nullable = false)
    val layoutType: LayoutType,
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    val author: User,
    
    @OneToMany(mappedBy = "spread", cascade = [CascadeType.ALL], orphanRemoval = true)
    val spreadCards: MutableList<SpreadCard> = mutableListOf(),
    
    @OneToMany(mappedBy = "spread", cascade = [CascadeType.ALL], orphanRemoval = true)
    val interpretations: MutableList<Interpretation> = mutableListOf()
)