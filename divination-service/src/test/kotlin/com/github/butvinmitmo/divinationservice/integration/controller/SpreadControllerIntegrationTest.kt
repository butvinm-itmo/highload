package com.github.butvinmitmo.divinationservice.integration.controller

import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.DeleteRequest
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.util.UUID
import com.github.tomakehurst.wiremock.client.WireMock.get as wireMockGet

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
class SpreadControllerIntegrationTest : BaseControllerIntegrationTest() {
    @BeforeEach
    fun setupWireMock() {
        // WireMock reset handled by BaseControllerIntegrationTest.resetWireMockBase()
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
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "id": "$oneCardLayoutId",
                                "name": "ONE_CARD",
                                "cardsCount": 1
                            }
                            """.trimIndent(),
                        ),
                ),
        )

        wireMock.stubFor(
            wireMockGet(urlEqualTo("/api/v0.0.1/layout-types/$threeCardsLayoutId"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "id": "$threeCardsLayoutId",
                                "name": "THREE_CARDS",
                                "cardsCount": 3
                            }
                            """.trimIndent(),
                        ),
                ),
        )

        wireMock.stubFor(
            wireMockGet(urlPathMatching("/api/v0.0.1/cards/random.*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            [
                                {
                                    "id": "00000000-0000-0000-0000-000000000030",
                                    "name": "The Fool",
                                    "arcanaType": {"id": "00000000-0000-0000-0000-000000000010", "name": "MAJOR"}
                                },
                                {
                                    "id": "00000000-0000-0000-0000-000000000031",
                                    "name": "The Magician",
                                    "arcanaType": {"id": "00000000-0000-0000-0000-000000000010", "name": "MAJOR"}
                                },
                                {
                                    "id": "00000000-0000-0000-0000-000000000032",
                                    "name": "The High Priestess",
                                    "arcanaType": {"id": "00000000-0000-0000-0000-000000000010", "name": "MAJOR"}
                                }
                            ]
                            """.trimIndent(),
                        ),
                ),
        )
    }

    @Test
    @Disabled(
        "TODO: Fix WireMock/Feign integration - Feign clients not picking up WireMock URL from @DynamicPropertySource",
    )
    fun `createSpread should create spread and return 201`() {
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
            .expectStatus()
            .isCreated
            .expectBody()
            .jsonPath("$.id")
            .exists()
    }

    @Test
    fun `getSpreads should return paginated spreads`() {
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

        webTestClient
            .get()
            .uri("/api/v0.0.1/spreads?page=0&size=10")
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .exists("X-Total-Count")
            .expectBody()
            .jsonPath("$")
            .isArray
    }

    @Test
    @Disabled("TODO: Fix WireMock/Feign integration - depends on createSpread which uses Feign")
    fun `getSpread should return spread details`() {
        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        val spreadId =
            webTestClient
                .post()
                .uri("/api/v0.0.1/spreads")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody()
                .jsonPath("$.id")
                .exists()
                .returnResult()
                .responseBody
                ?.let { body ->
                    objectMapper.readTree(body).get("id").asText()
                }!!

        webTestClient
            .get()
            .uri("/api/v0.0.1/spreads/$spreadId")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.id")
            .isEqualTo(spreadId)
            .jsonPath("$.question")
            .isEqualTo("Test question")
    }

    @Test
    fun `getSpread should return 404 when spread not found`() {
        val nonExistentId = UUID.randomUUID()

        webTestClient
            .get()
            .uri("/api/v0.0.1/spreads/$nonExistentId")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    @Disabled("TODO: Fix WireMock/Feign integration - depends on createSpread which uses Feign")
    fun `deleteSpread should delete spread when user is author`() {
        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        val spreadId =
            webTestClient
                .post()
                .uri("/api/v0.0.1/spreads")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody()
                .jsonPath("$.id")
                .exists()
                .returnResult()
                .responseBody
                ?.let { body ->
                    objectMapper.readTree(body).get("id").asText()
                }!!

        val deleteRequest = DeleteRequest(userId = testUserId)

        webTestClient
            .method(org.springframework.http.HttpMethod.DELETE)
            .uri("/api/v0.0.1/spreads/$spreadId")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(deleteRequest)
            .exchange()
            .expectStatus()
            .isNoContent
    }

    @Test
    @Disabled("TODO: Fix WireMock/Feign integration - depends on createSpread which uses Feign")
    fun `deleteSpread should return 403 when user is not author`() {
        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        val spreadId =
            webTestClient
                .post()
                .uri("/api/v0.0.1/spreads")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody()
                .jsonPath("$.id")
                .exists()
                .returnResult()
                .responseBody
                ?.let { body ->
                    objectMapper.readTree(body).get("id").asText()
                }!!

        val deleteRequest = DeleteRequest(userId = UUID.randomUUID())

        webTestClient
            .method(org.springframework.http.HttpMethod.DELETE)
            .uri("/api/v0.0.1/spreads/$spreadId")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(deleteRequest)
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `getSpreadsByScroll should return spreads with cursor`() {
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

        webTestClient
            .get()
            .uri("/api/v0.0.1/spreads/scroll?size=10")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$")
            .isArray
    }
}
