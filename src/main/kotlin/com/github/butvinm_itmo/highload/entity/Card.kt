package com.github.butvinm_itmo.highload.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "card")
data class Card(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID,
    
    @Column(nullable = false, length = 128)
    val name: String,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arcana_type_id", nullable = false)
    val arcanaType: ArcanaType
)