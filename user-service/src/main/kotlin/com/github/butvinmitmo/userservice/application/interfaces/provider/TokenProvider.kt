package com.github.butvinmitmo.userservice.application.interfaces.provider

import com.github.butvinmitmo.userservice.domain.model.User
import java.time.Instant

data class TokenResult(
    val token: String,
    val expiresAt: Instant,
)

interface TokenProvider {
    fun generateToken(user: User): TokenResult
}
