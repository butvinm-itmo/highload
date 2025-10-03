package com.github.butvinmitmo.highload.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
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
    @field:NotBlank(message = "Interpretation text is required")
    @field:Size(min = 1, max = 50000, message = "Interpretation text must be between 1 and 50000 characters")
    val text: String,

    @field:NotNull(message = "Author ID is required")
    val authorId: UUID,
)

data class UpdateInterpretationRequest(
    @field:NotBlank(message = "Interpretation text is required")
    @field:Size(min = 1, max = 50000, message = "Interpretation text must be between 1 and 50000 characters")
    val text: String,
)
