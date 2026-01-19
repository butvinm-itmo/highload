package com.github.butvinmitmo.userservice.security

import com.github.butvinmitmo.userservice.entity.Role
import com.github.butvinmitmo.userservice.entity.User
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtUtil(
    @Value("\${jwt.secret}")
    private val secret: String,
    @Value("\${jwt.expiration-hours}")
    private val expirationHours: Long,
) {
    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateToken(
        user: User,
        role: Role,
    ): Pair<String, Instant> {
        val now = Instant.now()
        val expiresAt = now.plus(expirationHours, ChronoUnit.HOURS)

        val token =
            Jwts
                .builder()
                .subject(user.id.toString())
                .claim("username", user.username)
                .claim("role", role.name)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact()

        return Pair(token, expiresAt)
    }
}
