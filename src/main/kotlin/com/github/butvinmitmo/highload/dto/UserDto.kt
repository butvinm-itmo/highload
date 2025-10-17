package com.github.butvinmitmo.highload.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class UserDto(
    val id: UUID,
    val username: String,
    val createdAt: Instant,
)

data class CreateUserRequest(
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 1, max = 128, message = "Username must be between 1 and 128 characters")
    val username: String,
)

data class UpdateUserRequest(
    @field:Size(min = 1, max = 128, message = "Username must be between 1 and 128 characters")
    val username: String? = null,
)

data class CreateUserResponse(
    val id: UUID,
)
