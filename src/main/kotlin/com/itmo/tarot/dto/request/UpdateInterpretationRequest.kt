package com.itmo.tarot.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class UpdateInterpretationRequest(
    @field:NotNull(message = "User ID is required")
    val userId: Long,
    
    @field:NotBlank(message = "Text is required")
    @field:Size(max = 2000, message = "Text must not exceed 2000 characters")
    val text: String
)