package com.github.butvinmitmo.shared.dto

import jakarta.validation.constraints.NotBlank
import java.time.Instant

/**
 * Login request with credentials
 */
data class LoginRequest(
    @field:NotBlank(message = "Username is required")
    val username: String,
    @field:NotBlank(message = "Password is required")
    val password: String,
)

/**
 * JWT token response after successful authentication
 */
data class AuthTokenResponse(
    val token: String,
    val expiresAt: Instant,
    val username: String,
    val role: String,
)

/**
 * Role enum for shared use across services
 */
enum class UserRoleDto {
    USER,
    ADMIN,
}
