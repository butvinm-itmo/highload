package com.github.butvinmitmo.userservice.unit.security

import com.github.butvinmitmo.userservice.TestEntityFactory
import com.github.butvinmitmo.userservice.infrastructure.security.JwtTokenProvider
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class JwtUtilTest {
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private val testSecret = "testSecretKeyThatIsLongEnoughForHS256AlgorithmRequirements!!"
    private val expirationHours = 24L
    private val userId = UUID.randomUUID()
    private val testUserRole = TestEntityFactory.testUserRole

    @BeforeEach
    fun setup() {
        jwtTokenProvider = JwtTokenProvider(testSecret, expirationHours)
    }

    @Test
    fun `generateToken should return token with correct expiration time`() {
        val user = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = Instant.now())
        val beforeGeneration = Instant.now()

        val result = jwtTokenProvider.generateToken(user)

        assertNotNull(result.token)
        assertNotNull(result.expiresAt)

        val expectedExpiration = beforeGeneration.plus(expirationHours, ChronoUnit.HOURS)
        val difference = ChronoUnit.SECONDS.between(result.expiresAt, expectedExpiration)
        assertTrue(difference < 2, "Expiration time should be within 2 seconds of expected value")
    }

    @Test
    fun `generateToken should include user ID as subject`() {
        val user = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = Instant.now())

        val result = jwtTokenProvider.generateToken(user)

        val secretKey = Keys.hmacShaKeyFor(testSecret.toByteArray())
        val claims =
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(result.token)
                .payload

        assertEquals(userId.toString(), claims.subject)
    }

    @Test
    fun `generateToken should include username claim`() {
        val user = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = Instant.now())

        val result = jwtTokenProvider.generateToken(user)

        val secretKey = Keys.hmacShaKeyFor(testSecret.toByteArray())
        val claims =
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(result.token)
                .payload

        assertEquals("testuser", claims["username"])
    }

    @Test
    fun `generateToken should include role claim`() {
        val user = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = Instant.now())

        val result = jwtTokenProvider.generateToken(user)

        val secretKey = Keys.hmacShaKeyFor(testSecret.toByteArray())
        val claims =
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(result.token)
                .payload

        assertEquals("USER", claims["role"])
    }

    @Test
    fun `generateToken should set issuedAt to current time`() {
        val user = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = Instant.now())
        val beforeGeneration = Instant.now()

        val result = jwtTokenProvider.generateToken(user)

        val secretKey = Keys.hmacShaKeyFor(testSecret.toByteArray())
        val claims =
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(result.token)
                .payload

        val issuedAt = claims.issuedAt.toInstant()
        val difference = ChronoUnit.SECONDS.between(beforeGeneration, issuedAt)
        assertTrue(difference < 2, "IssuedAt should be within 2 seconds of current time")
    }

    @Test
    fun `generateToken should be verifiable with correct secret`() {
        val user = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = Instant.now())

        val result = jwtTokenProvider.generateToken(user)

        val secretKey = Keys.hmacShaKeyFor(testSecret.toByteArray())
        val claims =
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(result.token)

        assertNotNull(claims)
    }

    @Test
    fun `generateToken should include expiration claim matching returned instant`() {
        val user = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = Instant.now())

        val result = jwtTokenProvider.generateToken(user)

        val secretKey = Keys.hmacShaKeyFor(testSecret.toByteArray())
        val claims =
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(result.token)
                .payload

        val tokenExpiration = claims.expiration.toInstant()
        assertEquals(result.expiresAt.epochSecond, tokenExpiration.epochSecond)
    }
}
