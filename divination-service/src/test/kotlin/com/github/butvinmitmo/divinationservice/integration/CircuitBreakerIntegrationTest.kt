package com.github.butvinmitmo.divinationservice.integration

import com.github.butvinmitmo.divinationservice.integration.controller.BaseControllerIntegrationTest
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.UserDto
import feign.FeignException
import feign.Request
import feign.RequestTemplate
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import java.time.Instant

@ActiveProfiles("test", "circuitbreaker")
class CircuitBreakerIntegrationTest : BaseControllerIntegrationTest() {
    @Autowired
    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    @AfterEach
    fun resetCircuitBreakers() {
        circuitBreakerRegistry.allCircuitBreakers.forEach { circuitBreaker ->
            circuitBreaker.reset()
            circuitBreaker.transitionToClosedState()
        }
    }

    private fun createFeignException(
        status: Int,
        message: String,
    ): FeignException {
        val request =
            Request.create(
                Request.HttpMethod.GET,
                "http://test",
                emptyMap(),
                null,
                RequestTemplate(),
            )
        return FeignException.errorStatus(
            message,
            feign.Response
                .builder()
                .status(status)
                .reason(message)
                .request(request)
                .headers(emptyMap())
                .body("""{"error": "$message"}""".toByteArray())
                .build(),
        )
    }

    @Test
    fun `should return 502 when user-service returns 503`() {
        // Mock user service to throw 503 Service Unavailable
        `when`(userServiceClient.getUserById(testUserId))
            .thenThrow(createFeignException(503, "Service Unavailable"))

        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads")
            .header("X-User-Id", testUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isEqualTo(502)
            .expectBody()
            .jsonPath("$.error")
            .isEqualTo("BAD_GATEWAY")
    }

    @Test
    fun `should return error when user-service times out`() {
        // Mock user service to sleep and cause timeout (4 seconds to trigger the 3s timeout)
        `when`(userServiceClient.getUserById(testUserId)).thenAnswer {
            Thread.sleep(4000) // Sleep longer than the 3s timeout configured in test profile
            ResponseEntity.ok(
                UserDto(
                    id = testUserId,
                    username = "admin",
                    createdAt = Instant.parse("2024-01-01T00:00:00Z"),
                    role = "USER",
                ),
            )
        }

        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        // Timeout causes error - time limiter should return 502 after configured timeout
        // Set WebTestClient timeout to 8s (longer than the service timeout)
        val result =
            webTestClient
                .mutate()
                .responseTimeout(java.time.Duration.ofSeconds(8))
                .build()
                .post()
                .uri("/api/v0.0.1/spreads")
                .header("X-User-Id", testUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .returnResult(String::class.java)

        // Verify we don't get a successful response
        assert(result.status.value() != 200 && result.status.value() != 201) {
            "Expected error status but got ${result.status.value()}"
        }
    }

    @Test
    fun `should return 404 for non-existent user - not counted as circuit breaker failure`() {
        // Mock user service to throw 404 Not Found
        `when`(userServiceClient.getUserById(testUserId))
            .thenThrow(createFeignException(404, "User not found"))

        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads")
            .header("X-User-Id", testUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody()
            .jsonPath("$.error")
            .isEqualTo("NOT_FOUND")
    }

    @Test
    fun `should return 502 when tarot-service returns 503`() {
        // Mock user service to return success
        val userDto =
            UserDto(
                id = testUserId,
                username = "admin",
                createdAt = Instant.parse("2024-01-01T00:00:00Z"),
                role = "USER",
            )
        `when`(userServiceClient.getUserById(testUserId)).thenReturn(ResponseEntity.ok(userDto))

        // Mock tarot service to throw 503 Service Unavailable
        `when`(tarotServiceClient.getLayoutTypeById(oneCardLayoutId))
            .thenThrow(createFeignException(503, "Service Unavailable"))

        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads")
            .header("X-User-Id", testUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isEqualTo(502)
            .expectBody()
            .jsonPath("$.error")
            .isEqualTo("BAD_GATEWAY")
    }

    @Test
    fun `should return 502 when user-service returns 500`() {
        // Mock user service to throw 500 Internal Server Error
        `when`(userServiceClient.getUserById(testUserId))
            .thenThrow(createFeignException(500, "Internal Server Error"))

        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads")
            .header("X-User-Id", testUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isEqualTo(502)
            .expectBody()
            .jsonPath("$.error")
            .isEqualTo("BAD_GATEWAY")
    }
}
