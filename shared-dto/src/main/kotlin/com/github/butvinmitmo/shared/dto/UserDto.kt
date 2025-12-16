package com.github.butvinmitmo.shared.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class UserDto(
    val id: UUID,
    val username: String,
    val role: String,
    val createdAt: Instant,
)

data class CreateUserRequest(
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 1, max = 128, message = "Username must be between 1 and 128 characters")
    val username: String,
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&#])[A-Za-z\\d@\$!%*?&#]+$",
        message =
            "Password must contain at least one uppercase letter, one lowercase letter, " +
                "one digit, and one special character (@\$!%*?&#)",
    )
    val password: String,
)

data class UpdateUserRequest(
    @field:Size(min = 1, max = 128, message = "Username must be between 1 and 128 characters")
    val username: String? = null,
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&#])[A-Za-z\\d@\$!%*?&#]+$",
        message =
            "Password must contain at least one uppercase letter, one lowercase letter, " +
                "one digit, and one special character",
    )
    val password: String? = null,
)

data class CreateUserResponse(
    val id: UUID,
)
