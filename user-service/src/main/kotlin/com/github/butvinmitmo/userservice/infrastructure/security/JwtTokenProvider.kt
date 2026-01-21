package com.github.butvinmitmo.userservice.infrastructure.security

import com.github.butvinmitmo.userservice.application.interfaces.provider.TokenProvider
import com.github.butvinmitmo.userservice.application.interfaces.provider.TokenResult
import com.github.butvinmitmo.userservice.domain.model.User
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val secret: String,
    @Value("\${jwt.expiration-hours}")
    private val expirationHours: Long,
) : TokenProvider {
    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    override fun generateToken(user: User): TokenResult {
        val now = Instant.now()
        val expiresAt = now.plus(expirationHours, ChronoUnit.HOURS)

        val token =
            Jwts
                .builder()
                .subject(user.id!!.toString())
                .claim("username", user.username)
                .claim("role", user.role.name)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact()

        return TokenResult(token, expiresAt)
    }
}
