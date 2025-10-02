package com.github.butvinmitmo.highload.dto

import java.util.UUID

data class ArcanaTypeDto(
    val id: UUID,
    val name: String,
)

enum class ArcanaType {
    MAJOR,
    MINOR,
}
