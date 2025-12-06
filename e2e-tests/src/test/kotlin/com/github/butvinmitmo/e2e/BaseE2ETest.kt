package com.github.butvinmitmo.e2e

import com.github.butvinmitmo.e2e.client.DivinationServiceClient
import com.github.butvinmitmo.e2e.client.TarotServiceClient
import com.github.butvinmitmo.e2e.client.UserServiceClient
import feign.FeignException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.net.HttpURLConnection
import java.net.URI

/**
 * Base class for E2E tests.
 *
 * Tests connect to services running via docker compose.
 * Start services before running tests: `docker compose up -d`
 *
 * Service URLs are configured in application.yml and can be overridden
 * via environment variables or system properties.
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
        private const val USER_SERVICE_URL = "http://localhost:8081"
        private const val TAROT_SERVICE_URL = "http://localhost:8082"
        private const val DIVINATION_SERVICE_URL = "http://localhost:8083"

        @JvmStatic
        @BeforeAll
        fun waitForServices() {
            val services =
                listOf(
                    USER_SERVICE_URL to "user-service",
                    TAROT_SERVICE_URL to "tarot-service",
                    DIVINATION_SERVICE_URL to "divination-service",
                )

            val maxAttempts = 60
            val delayMs = 2000L

            for ((url, name) in services) {
                var healthy = false
                for (attempt in 1..maxAttempts) {
                    try {
                        val connection = URI("$url/actuator/health").toURL().openConnection() as HttpURLConnection
                        connection.connectTimeout = 2000
                        connection.readTimeout = 2000
                        connection.requestMethod = "GET"

                        if (connection.responseCode == 200) {
                            healthy = true
                            println("$name is healthy")
                            break
                        }
                    } catch (e: Exception) {
                        // Service not ready yet
                    }

                    if (attempt < maxAttempts) {
                        println("Waiting for $name... (attempt $attempt/$maxAttempts)")
                        Thread.sleep(delayMs)
                    }
                }

                if (!healthy) {
                    throw IllegalStateException(
                        "$name is not healthy after $maxAttempts attempts. " +
                            "Make sure services are running: docker compose up -d",
                    )
                }
            }
        }
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
