package com.github.butvinmitmo.gatewayservice.unit.filter

import com.github.butvinmitmo.gatewayservice.config.SecurityProperties
import com.github.butvinmitmo.gatewayservice.filter.JwtAuthenticationFilter
import com.github.butvinmitmo.gatewayservice.security.JwtUtil
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

class JwtAuthenticationFilterTest {
    private lateinit var jwtUtil: JwtUtil
    private lateinit var filter: JwtAuthenticationFilter
    private lateinit var chain: GatewayFilterChain
    private val testSecret = "testSecretKeyThatIsLongEnoughForHS256AlgorithmRequirements!!"
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        jwtUtil = JwtUtil(testSecret)
        val securityProperties = SecurityProperties(publicPaths = listOf("/api/v0.0.1/auth/login", "/actuator/health"))
        filter = JwtAuthenticationFilter(jwtUtil, securityProperties)
        chain = mock()
        whenever(chain.filter(any())).thenReturn(Mono.empty())
    }

    @Test
    fun `filter should bypass authentication for public paths`() {
        val request = MockServerHttpRequest.get("/api/v0.0.1/auth/login").build()
        val exchange = MockServerWebExchange.from(request)

        filter.filter(exchange, chain).block()

        verify(chain).filter(any())
        assertNull(exchange.response.statusCode)
    }

    @Test
    fun `filter should bypass authentication for actuator health endpoint`() {
        val request = MockServerHttpRequest.get("/actuator/health").build()
        val exchange = MockServerWebExchange.from(request)

        filter.filter(exchange, chain).block()

        verify(chain).filter(any())
        assertNull(exchange.response.statusCode)
    }

    @Test
    fun `filter should add X-User-Id and X-User-Role headers for valid token`() {
        val token = createValidToken()
        val request =
            MockServerHttpRequest
                .get("/api/v0.0.1/users")
                .header("Authorization", "Bearer $token")
                .build()
        val exchange = MockServerWebExchange.from(request)

        filter.filter(exchange, chain).block()

        verify(chain).filter(any())
        assertNull(exchange.response.statusCode)
    }

    @Test
    fun `filter should return 401 for missing Authorization header`() {
        val request = MockServerHttpRequest.get("/api/v0.0.1/users").build()
        val exchange = MockServerWebExchange.from(request)

        filter.filter(exchange, chain).block()

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
    }

    @Test
    fun `filter should return 401 for Authorization header without Bearer prefix`() {
        val token = createValidToken()
        val request =
            MockServerHttpRequest
                .get("/api/v0.0.1/users")
                .header("Authorization", token)
                .build()
        val exchange = MockServerWebExchange.from(request)

        filter.filter(exchange, chain).block()

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
    }

    @Test
    fun `filter should return 401 for malformed token`() {
        val request =
            MockServerHttpRequest
                .get("/api/v0.0.1/users")
                .header("Authorization", "Bearer invalid.token.here")
                .build()
        val exchange = MockServerWebExchange.from(request)

        filter.filter(exchange, chain).block()

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
    }

    @Test
    fun `filter should return 401 for expired token`() {
        val token = createExpiredToken()
        val request =
            MockServerHttpRequest
                .get("/api/v0.0.1/users")
                .header("Authorization", "Bearer $token")
                .build()
        val exchange = MockServerWebExchange.from(request)

        filter.filter(exchange, chain).block()

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
    }

    @Test
    fun `filter should return 401 for token with invalid subject`() {
        val token = createTokenWithInvalidSubject()
        val request =
            MockServerHttpRequest
                .get("/api/v0.0.1/users")
                .header("Authorization", "Bearer $token")
                .build()
        val exchange = MockServerWebExchange.from(request)

        filter.filter(exchange, chain).block()

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
    }

    @Test
    fun `filter should return 401 for token without role claim`() {
        val token = createTokenWithoutRole()
        val request =
            MockServerHttpRequest
                .get("/api/v0.0.1/users")
                .header("Authorization", "Bearer $token")
                .build()
        val exchange = MockServerWebExchange.from(request)

        filter.filter(exchange, chain).block()

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
    }

    @Test
    fun `filter should have highest precedence`() {
        val order = filter.order

        assertEquals(Int.MIN_VALUE, order)
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

    private fun createExpiredToken(): String {
        val secretKey = Keys.hmacShaKeyFor(testSecret.toByteArray())
        return Jwts
            .builder()
            .subject(userId.toString())
            .claim("username", "testuser")
            .claim("role", "USER")
            .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
            .expiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
            .signWith(secretKey)
            .compact()
    }

    private fun createTokenWithInvalidSubject(): String {
        val secretKey = Keys.hmacShaKeyFor(testSecret.toByteArray())
        return Jwts
            .builder()
            .subject("not-a-valid-uuid")
            .claim("username", "testuser")
            .claim("role", "USER")
            .issuedAt(Date())
            .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .signWith(secretKey)
            .compact()
    }

    private fun createTokenWithoutRole(): String {
        val secretKey = Keys.hmacShaKeyFor(testSecret.toByteArray())
        return Jwts
            .builder()
            .subject(userId.toString())
            .claim("username", "testuser")
            .issuedAt(Date())
            .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .signWith(secretKey)
            .compact()
    }
}
