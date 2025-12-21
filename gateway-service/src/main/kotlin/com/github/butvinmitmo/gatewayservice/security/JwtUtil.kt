package com.github.butvinmitmo.gatewayservice.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtUtil(
    @Value("\${jwt.secret}")
    private val secret: String,
) {
    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun validateAndExtract(token: String): Claims? =
        try {
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: Exception) {
            null // Invalid token
        }

    fun getUserId(claims: Claims): UUID? =
        try {
            UUID.fromString(claims.subject)
        } catch (e: Exception) {
            null
        }

    fun getRole(claims: Claims): String? = claims["role"] as? String
}
