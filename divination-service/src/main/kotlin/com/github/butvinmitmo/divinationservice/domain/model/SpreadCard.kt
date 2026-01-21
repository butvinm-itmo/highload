package com.github.butvinmitmo.divinationservice.domain.model

import java.util.UUID

data class SpreadCard(
    val id: UUID?,
    val spreadId: UUID,
    val cardId: UUID,
    val positionInSpread: Int,
    val isReversed: Boolean,
)
