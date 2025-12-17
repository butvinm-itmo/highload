package com.github.butvinmitmo.e2e

import com.github.butvinmitmo.e2e.config.AuthContext
import com.github.butvinmitmo.shared.client.DivinationServiceClient
import com.github.butvinmitmo.shared.client.TarotServiceClient
import com.github.butvinmitmo.shared.client.UserServiceClient
import com.github.butvinmitmo.shared.dto.LoginRequest
import feign.FeignException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.client.RestTemplate

/**
 * Base class for E2E tests.
 *
 * Tests require services to be running before execution.
 * Start services with: docker compose up -d
 */
@SpringBootTest(classes = [E2ETestApplication::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseE2ETest {
    @Autowired
    protected lateinit var userClient: UserServiceClient

    @Autowired
    protected lateinit var tarotClient: TarotServiceClient

    @Autowired
    protected lateinit var divinationClient: DivinationServiceClient

    companion object {
        private const val DEFAULT_GATEWAY_URL = "http://localhost:8080"
        private const val HEALTH_CHECK_TIMEOUT_MS = 5000L
        private const val HEALTH_CHECK_MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L

        @JvmStatic
        @BeforeAll
        fun verifyServicesRunning() {
            val gatewayUrl =
                System.getProperty("GATEWAY_URL")
                    ?: System.getenv("GATEWAY_URL")
                    ?: DEFAULT_GATEWAY_URL

            println("═══════════════════════════════════════════════════════════════════")
            println("E2E Tests - Pre-Running Application Mode")
            println("═══════════════════════════════════════════════════════════════════")
            println("Gateway URL: $gatewayUrl")
            println("Checking if services are running...")
            println("═══════════════════════════════════════════════════════════════════")

            val restTemplate = RestTemplate()
            var lastException: Exception? = null

            for (attempt in 1..HEALTH_CHECK_MAX_RETRIES) {
                try {
                    val response =
                        restTemplate.getForEntity(
                            "$gatewayUrl/actuator/health",
                            String::class.java,
                        )

                    if (response.statusCode.is2xxSuccessful) {
                        println("✓ Gateway health check passed")
                        println("═══════════════════════════════════════════════════════════════════")
                        return
                    }
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < HEALTH_CHECK_MAX_RETRIES) {
                        println("⚠ Health check attempt $attempt/$HEALTH_CHECK_MAX_RETRIES failed, retrying...")
                        Thread.sleep(RETRY_DELAY_MS)
                    }
                }
            }

            // All retries failed
            println("✗ Gateway health check failed after $HEALTH_CHECK_MAX_RETRIES attempts")
            println("═══════════════════════════════════════════════════════════════════")
            println("ERROR: Services are not running!")
            println("")
            println("To start services, run:")
            println("  docker compose up -d")
            println("")
            println("Or with build:")
            println("  docker compose up -d --build")
            println("")
            println("To check service status:")
            println("  docker compose ps")
            println("  curl $gatewayUrl/actuator/health")
            println("═══════════════════════════════════════════════════════════════════")

            throw IllegalStateException(
                "Gateway is not accessible at $gatewayUrl/actuator/health. " +
                    "Please start services with 'docker compose up -d'. " +
                    "Last error: ${lastException?.message}",
                lastException,
            )
        }
    }

    /**
     * Login with given credentials and set the JWT token in AuthContext
     */
    protected fun loginAndSetToken(
        username: String,
        password: String,
    ): String {
        val request = LoginRequest(username = username, password = password)
        val response = userClient.login(request)
        val token = response.body!!.token
        AuthContext.setToken(token)
        return token
    }

    /**
     * Login as the default admin user
     */
    protected fun loginAsAdmin(): String = loginAndSetToken("admin", "Admin@123")

    /**
     * Clear the authentication token
     */
    protected fun clearAuth() {
        AuthContext.clear()
    }

    /**
     * Clean up authentication context after each test
     */
    @AfterEach
    fun cleanupAuth() {
        AuthContext.clear()
    }

    protected fun assertThrowsWithStatus(
        expectedStatus: Int,
        block: () -> Any,
    ): FeignException {
        val exception = assertThrows<FeignException> { block() }
        assertEquals(
            expectedStatus,
            exception.status(),
            "Expected HTTP status $expectedStatus but got ${exception.status()}",
        )
        return exception
    }
}
