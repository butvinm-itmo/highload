package com.github.butvinmitmo.tarotservice.domain.model

import java.util.UUID

data class Card(
    val id: UUID,
    val name: String,
    val arcanaType: ArcanaType,
)
