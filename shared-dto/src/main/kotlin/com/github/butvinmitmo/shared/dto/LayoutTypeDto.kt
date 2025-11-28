package com.github.butvinmitmo.shared.dto

import java.util.UUID

data class LayoutTypeDto(
    val id: UUID,
    val name: String,
    val cardsCount: Int,
)
