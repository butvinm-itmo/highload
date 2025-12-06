package com.github.butvinmitmo.shared.dto

import java.util.UUID

data class CardDto(
    val id: UUID,
    val name: String,
    val arcanaType: ArcanaTypeDto,
)
