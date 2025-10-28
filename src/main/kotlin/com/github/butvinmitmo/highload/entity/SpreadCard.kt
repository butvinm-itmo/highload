package com.github.butvinmitmo.highload.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import java.util.UUID

@Entity
@Table(name = "spread_card")
class SpreadCard(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spread_id", nullable = false)
    @JsonBackReference
    val spread: Spread,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    val card: Card,
    @Column(name = "position_in_spread", nullable = false)
    val positionInSpread: Int,
    @Column(name = "is_reversed", nullable = false)
    val isReversed: Boolean,
) {
    @Id
    @Generated
    @Column(columnDefinition = "uuid", insertable = false, nullable = false)
    lateinit var id: UUID
}
