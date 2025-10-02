package com.github.butvinm_itmo.highload.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "spread_card")
data class SpreadCard(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    val id: UUID? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spread_id", nullable = false)
    val spread: Spread,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    val card: Card,
    
    @Column(name = "position_in_spread", nullable = false)
    val positionInSpread: Int,
    
    @Column(name = "is_reversed", nullable = false)
    val isReversed: Boolean = false
)