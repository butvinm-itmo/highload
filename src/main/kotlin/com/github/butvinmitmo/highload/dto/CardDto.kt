package com.github.butvinmitmo.highload.dto

import java.util.UUID

data class CardDto(
    val id: UUID,
    val name: String,
    val arcanaType: ArcanaTypeDto,
)

data class CardSummaryDto(
    val id: UUID,
    val name: String,
    val arcanaTypeName: String,
)
