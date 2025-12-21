package com.github.butvinmitmo.gatewayservice.unit.security

import com.github.butvinmitmo.gatewayservice.security.JwtUtil
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

class JwtUtilTest {
    private lateinit var jwtUtil: JwtUtil
    private val testSecret = "testSecretKeyThatIsLongEnoughForHS256AlgorithmRequirements!!"
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        jwtUtil = JwtUtil(testSecret)
    }

    @Test
    fun `validateAndExtract should return claims for valid token`() {
        val token = createValidToken()

        val claims = jwtUtil.validateAndExtract(token)

        assertNotNull(claims)
        assertEquals(userId.toString(), claims!!.subject)
        assertEquals("testuser", claims["username"])
        assertEquals("USER", claims["role"])
    }

    @Test
    fun `validateAndExtract should return null for malformed token`() {
        val invalidToken = "not.a.valid.jwt.token"

        val claims = jwtUtil.validateAndExtract(invalidToken)

        assertNull(claims)
    }

    @Test
    fun `validateAndExtract should return null for token with wrong signature`() {
        val wrongSecret = "wrongSecretKeyThatIsAlsoLongEnoughForHS256AlgoRequirements!"
        val secretKey = Keys.hmacShaKeyFor(wrongSecret.toByteArray())
        val token =
            Jwts
                .builder()
                .subject(userId.toString())
                .claim("username", "testuser")
                .claim("role", "USER")
                .issuedAt(Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact()

        val claims = jwtUtil.validateAndExtract(token)

        assertNull(claims)
    }

    @Test
    fun `validateAndExtract should return null for expired token`() {
        val secretKey = Keys.hmacShaKeyFor(testSecret.toByteArray())
        val token =
            Jwts
                .builder()
                .subject(userId.toString())
                .claim("username", "testuser")
                .claim("role", "USER")
                .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact()

        val claims = jwtUtil.validateAndExtract(token)

        assertNull(claims)
    }

    @Test
    fun `getUserId should extract UUID from claims`() {
        val token = createValidToken()
        val claims = jwtUtil.validateAndExtract(token)!!

        val extractedUserId = jwtUtil.getUserId(claims)

        assertEquals(userId, extractedUserId)
    }

    @Test
    fun `getUserId should return null for invalid UUID in subject`() {
        val secretKey = Keys.hmacShaKeyFor(testSecret.toByteArray())
        val token =
            Jwts
                .builder()
                .subject("not-a-valid-uuid")
                .claim("username", "testuser")
                .claim("role", "USER")
                .issuedAt(Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact()

        val claims = jwtUtil.validateAndExtract(token)!!
        val extractedUserId = jwtUtil.getUserId(claims)

        assertNull(extractedUserId)
    }

    @Test
    fun `getRole should extract role from claims`() {
        val token = createValidToken()
        val claims = jwtUtil.validateAndExtract(token)!!

        val role = jwtUtil.getRole(claims)

        assertEquals("USER", role)
    }

    @Test
    fun `getRole should return null when role claim is missing`() {
        val secretKey = Keys.hmacShaKeyFor(testSecret.toByteArray())
        val token =
            Jwts
                .builder()
                .subject(userId.toString())
                .claim("username", "testuser")
                .issuedAt(Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact()

        val claims = jwtUtil.validateAndExtract(token)!!
        val role = jwtUtil.getRole(claims)

        assertNull(role)
    }

    private fun createValidToken(): String {
        val secretKey = Keys.hmacShaKeyFor(testSecret.toByteArray())
        return Jwts
            .builder()
            .subject(userId.toString())
            .claim("username", "testuser")
            .claim("role", "USER")
            .issuedAt(Date())
            .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .signWith(secretKey)
            .compact()
    }
}
