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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import com.github.tomakehurst.wiremock.client.WireMock.get as wireMockGet

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CircuitBreakerIntegrationTest : BaseControllerIntegrationTest() {
    @BeforeEach
    fun resetWireMock() {
        wireMock.resetAll()
    }

    @Test
    fun `should return 502 when user-service returns 503`() {
        wireMock.stubFor(
            wireMockGet(urlPathMatching("/api/internal/users/.*"))
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

        mockMvc
            .perform(
                post("/api/v0.0.1/spreads")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.error").value("BAD_GATEWAY"))
    }

    @Test
    fun `should return error when user-service times out`() {
        wireMock.stubFor(
            wireMockGet(urlPathMatching("/api/internal/users/.*"))
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

        // Timeout causes error - either 404 (stub timeout) or 502 (Feign timeout)
        val result =
            mockMvc
                .perform(
                    post("/api/v0.0.1/spreads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andReturn()

        // Verify we don't get a successful response
        assert(result.response.status != 200 && result.response.status != 201) {
            "Expected error status but got ${result.response.status}"
        }
    }

    @Test
    fun `should return 404 for non-existent user - not counted as circuit breaker failure`() {
        wireMock.stubFor(
            wireMockGet(urlPathMatching("/api/internal/users/.*"))
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

        mockMvc
            .perform(
                post("/api/v0.0.1/spreads")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("NOT_FOUND"))
    }

    @Test
    fun `should return 502 when tarot-service returns 503`() {
        wireMock.stubFor(
            wireMockGet(urlPathMatching("/api/internal/users/.*"))
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
            wireMockGet(urlEqualTo("/api/internal/layout-types/$oneCardLayoutId"))
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

        mockMvc
            .perform(
                post("/api/v0.0.1/spreads")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.error").value("BAD_GATEWAY"))
    }

    @Test
    fun `should return 502 when user-service returns 500`() {
        wireMock.stubFor(
            wireMockGet(urlPathMatching("/api/internal/users/.*"))
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

        mockMvc
            .perform(
                post("/api/v0.0.1/spreads")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.error").value("BAD_GATEWAY"))
    }
}
