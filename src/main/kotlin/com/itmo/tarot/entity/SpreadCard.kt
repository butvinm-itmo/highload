package com.itmo.tarot.entity

import jakarta.persistence.*

@Entity
@Table(name = "spread_cards")
data class SpreadCard(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spread_id", nullable = false)
    val spread: Spread,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    val card: Card,
    
    @Column(name = "position_in_spread", nullable = false)
    val positionInSpread: Int,
    
    @Column(name = "is_reversed", nullable = false)
    val isReversed: Boolean
)