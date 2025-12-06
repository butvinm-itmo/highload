package com.github.butvinmitmo.divinationservice.integration.controller

import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.DeleteRequest
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import com.github.tomakehurst.wiremock.client.WireMock.get as wireMockGet

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpreadControllerIntegrationTest : BaseControllerIntegrationTest() {
    @BeforeEach
    fun setupWireMock() {
        wireMock.resetAll()
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
            wireMockGet(urlEqualTo("/api/internal/layout-types/$threeCardsLayoutId"))
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
            wireMockGet(urlPathMatching("/api/internal/cards/random.*"))
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
    fun `createSpread should create spread and return 201`() {
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
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
    }

    @Test
    fun `getSpreads should return paginated spreads`() {
        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        mockMvc.perform(
            post("/api/v0.0.1/spreads")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )

        mockMvc
            .perform(get("/api/v0.0.1/spreads?page=0&size=10"))
            .andExpect(status().isOk)
            .andExpect(header().exists("X-Total-Count"))
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `getSpread should return spread details`() {
        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        val result =
            mockMvc
                .perform(
                    post("/api/v0.0.1/spreads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andReturn()

        val spreadId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

        mockMvc
            .perform(get("/api/v0.0.1/spreads/$spreadId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(spreadId))
            .andExpect(jsonPath("$.question").value("Test question"))
    }

    @Test
    fun `getSpread should return 404 when spread not found`() {
        val nonExistentId = UUID.randomUUID()

        mockMvc
            .perform(get("/api/v0.0.1/spreads/$nonExistentId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `deleteSpread should delete spread when user is author`() {
        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        val result =
            mockMvc
                .perform(
                    post("/api/v0.0.1/spreads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andReturn()

        val spreadId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

        val deleteRequest = DeleteRequest(userId = testUserId)

        mockMvc
            .perform(
                delete("/api/v0.0.1/spreads/$spreadId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(deleteRequest)),
            ).andExpect(status().isNoContent)
    }

    @Test
    fun `deleteSpread should return 403 when user is not author`() {
        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        val result =
            mockMvc
                .perform(
                    post("/api/v0.0.1/spreads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andReturn()

        val spreadId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

        val deleteRequest = DeleteRequest(userId = UUID.randomUUID())

        mockMvc
            .perform(
                delete("/api/v0.0.1/spreads/$spreadId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(deleteRequest)),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `getSpreadsByScroll should return spreads with cursor`() {
        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        mockMvc.perform(
            post("/api/v0.0.1/spreads")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )

        mockMvc
            .perform(get("/api/v0.0.1/spreads/scroll?size=10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }
}
