package com.github.butvinmitmo.highload.dto

import java.util.UUID

data class SpreadCardDto(
    val id: UUID,
    val card: CardDto,
    val positionInSpread: Int,
    val isReversed: Boolean,
)
