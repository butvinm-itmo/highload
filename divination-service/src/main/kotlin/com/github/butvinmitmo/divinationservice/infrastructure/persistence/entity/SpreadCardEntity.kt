package com.github.butvinmitmo.divinationservice.infrastructure.persistence.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("spread_card")
data class SpreadCardEntity(
    @Id
    val id: UUID? = null,
    @Column("spread_id")
    val spreadId: UUID,
    @Column("card_id")
    val cardId: UUID,
    @Column("position_in_spread")
    val positionInSpread: Int,
    @Column("is_reversed")
    val isReversed: Boolean,
)
