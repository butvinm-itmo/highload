package com.github.butvinmitmo.shared.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class InterpretationDto(
    val id: UUID,
    val text: String,
    val createdAt: Instant,
    val author: UserDto,
    val spreadId: UUID,
    val fileUrl: String? = null,
)

data class CreateInterpretationRequest(
    @field:NotBlank(message = "Interpretation text is required")
    @field:Size(min = 1, max = 50000, message = "Interpretation text must be between 1 and 50000 characters")
    val text: String,
)

data class CreateInterpretationResponse(
    val id: UUID,
)

data class UpdateInterpretationRequest(
    @field:NotBlank(message = "Interpretation text is required")
    @field:Size(min = 1, max = 50000, message = "Interpretation text must be between 1 and 50000 characters")
    val text: String,
)
