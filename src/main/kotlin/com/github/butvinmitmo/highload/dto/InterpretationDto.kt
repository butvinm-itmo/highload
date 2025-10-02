package com.github.butvinmitmo.highload.dto

import java.time.Instant
import java.util.UUID

data class InterpretationDto(
    val id: UUID,
    val text: String,
    val createdAt: Instant,
    val author: UserDto,
    val spreadId: UUID,
)

data class InterpretationSummaryDto(
    val id: UUID,
    val text: String,
    val createdAt: Instant,
    val authorUsername: String,
)

data class CreateInterpretationRequest(
    val text: String,
    val authorId: UUID,
)

data class UpdateInterpretationRequest(
    val text: String,
)
