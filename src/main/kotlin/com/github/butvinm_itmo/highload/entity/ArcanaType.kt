package com.github.butvinm_itmo.highload.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "arcana_type")
data class ArcanaType(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID,
    
    @Column(nullable = false, length = 16)
    val name: String
)