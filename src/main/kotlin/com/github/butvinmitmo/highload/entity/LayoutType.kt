package com.github.butvinmitmo.highload.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "layout_type")
data class LayoutType(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID,
    @Column(nullable = false, length = 32)
    val name: String,
    @Column(name = "cards_count", nullable = false)
    val cardsCount: Int,
)
