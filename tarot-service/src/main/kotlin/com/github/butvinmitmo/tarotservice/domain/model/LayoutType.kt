package com.github.butvinmitmo.tarotservice.domain.model

import java.util.UUID

data class LayoutType(
    val id: UUID,
    val name: String,
    val cardsCount: Int,
)
