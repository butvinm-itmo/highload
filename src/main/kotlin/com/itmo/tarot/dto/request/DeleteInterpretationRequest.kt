package com.itmo.tarot.dto.request

import jakarta.validation.constraints.NotNull

data class DeleteInterpretationRequest(
    @field:NotNull(message = "User ID is required")
    val userId: Long
)