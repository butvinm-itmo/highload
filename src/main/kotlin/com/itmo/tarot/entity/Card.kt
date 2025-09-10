package com.itmo.tarot.entity

import jakarta.persistence.*

@Entity
@Table(name = "cards")
data class Card(
    @Id
    val id: Int,
    
    @Column(nullable = false)
    val name: String,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "arcana_type", nullable = false)
    val arcanaType: ArcanaType,
    
    @OneToMany(mappedBy = "card", cascade = [CascadeType.ALL], orphanRemoval = true)
    val spreadCards: MutableList<SpreadCard> = mutableListOf()
)