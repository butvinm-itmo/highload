package com.itmo.tarot.dto.request

import com.itmo.tarot.entity.LayoutType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateSpreadRequest(
    @field:NotNull(message = "User ID is required")
    val userId: Long,
    
    @field:NotBlank(message = "Question is required")
    @field:Size(max = 1000, message = "Question must not exceed 1000 characters")
    val question: String,
    
    @field:NotNull(message = "Layout type is required")
    val layoutType: LayoutType
)