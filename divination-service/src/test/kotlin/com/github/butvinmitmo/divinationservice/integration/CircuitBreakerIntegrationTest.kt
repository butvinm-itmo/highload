package com.github.butvinmitmo.divinationservice.integration

import com.github.butvinmitmo.divinationservice.integration.controller.BaseControllerIntegrationTest
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import com.github.tomakehurst.wiremock.client.WireMock.get as wireMockGet

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test", "circuitbreaker")
class CircuitBreakerIntegrationTest : BaseControllerIntegrationTest() {
    @BeforeEach
    fun resetWireMock() {
        wireMock.resetAll()
    }

    @Test
    fun `should return 502 when user-service returns 503`() {
        wireMock.stubFor(
            wireMockGet(urlPathMatching("/api/v0.0.1/users/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"error": "Service Unavailable"}"""),
                ),
        )

        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(502)
            .expectBody()
            .jsonPath("$.error").isEqualTo("BAD_GATEWAY")
    }

    @Test
    fun `should return error when user-service times out`() {
        wireMock.stubFor(
            wireMockGet(urlPathMatching("/api/v0.0.1/users/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withFixedDelay(10000)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "id": "$testUserId",
                                "username": "admin",
                                "createdAt": "2024-01-01T00:00:00Z"
                            }
                            """.trimIndent(),
                        ),
                ),
        )

        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        // Timeout causes error - time limiter should return 502 after 5s
        // Set WebTestClient timeout to 10s (longer than time limiter's 5s)
        val result =
            webTestClient
                .mutate()
                .responseTimeout(java.time.Duration.ofSeconds(10))
                .build()
                .post()
                .uri("/api/v0.0.1/spreads")
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
        wireMock.stubFor(
            wireMockGet(urlPathMatching("/api/v0.0.1/users/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"error": "NOT_FOUND", "message": "User not found"}"""),
                ),
        )

        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.error").isEqualTo("NOT_FOUND")
    }

    @Test
    fun `should return 502 when tarot-service returns 503`() {
        wireMock.stubFor(
            wireMockGet(urlPathMatching("/api/v0.0.1/users/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "id": "$testUserId",
                                "username": "admin",
                                "createdAt": "2024-01-01T00:00:00Z"
                            }
                            """.trimIndent(),
                        ),
                ),
        )

        wireMock.stubFor(
            wireMockGet(urlEqualTo("/api/v0.0.1/layout-types/$oneCardLayoutId"))
                .willReturn(
                    aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"error": "Service Unavailable"}"""),
                ),
        )

        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(502)
            .expectBody()
            .jsonPath("$.error").isEqualTo("BAD_GATEWAY")
    }

    @Test
    fun `should return 502 when user-service returns 500`() {
        wireMock.stubFor(
            wireMockGet(urlPathMatching("/api/v0.0.1/users/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"error": "Internal Server Error"}"""),
                ),
        )

        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(502)
            .expectBody()
            .jsonPath("$.error").isEqualTo("BAD_GATEWAY")
    }
}
