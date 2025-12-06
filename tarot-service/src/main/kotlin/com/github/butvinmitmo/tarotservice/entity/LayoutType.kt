package com.github.butvinmitmo.tarotservice.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import java.util.UUID

@Entity
@Table(name = "layout_type")
class LayoutType(
    @Column(nullable = false, length = 32)
    val name: String,
    @Column(name = "cards_count", nullable = false)
    val cardsCount: Int,
) {
    @Id
    @Generated
    @Column(columnDefinition = "uuid", insertable = false, nullable = false)
    lateinit var id: UUID
}
