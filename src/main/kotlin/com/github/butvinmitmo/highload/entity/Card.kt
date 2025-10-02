package com.github.butvinmitmo.highload.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
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
    val arcanaType: ArcanaType,
)
