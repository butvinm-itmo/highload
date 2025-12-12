package com.github.butvinmitmo.divinationservice.integration.controller

import com.github.butvinmitmo.shared.dto.CreateInterpretationRequest
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.DeleteRequest
import com.github.butvinmitmo.shared.dto.UpdateInterpretationRequest
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import com.github.tomakehurst.wiremock.client.WireMock.get as wireMockGet

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InterpretationControllerIntegrationTest : BaseControllerIntegrationTest() {
    @BeforeEach
    fun setupWireMock() {
        wireMock.resetAll()
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
                                }
                            ]
                            """.trimIndent(),
                        ),
                ),
        )
    }

    private fun createSpread(): String {
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

        return objectMapper.readTree(result.response.contentAsString).get("id").asText()
    }

    @Test
    fun `addInterpretation should create interpretation and return 201`() {
        val spreadId = createSpread()
        val request = CreateInterpretationRequest(text = "Test interpretation", authorId = testUserId)

        mockMvc
            .perform(
                post("/api/v0.0.1/spreads/$spreadId/interpretations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
    }

    @Test
    fun `addInterpretation should return 409 when user already has interpretation`() {
        val spreadId = createSpread()
        val request = CreateInterpretationRequest(text = "Test interpretation", authorId = testUserId)

        mockMvc.perform(
            post("/api/v0.0.1/spreads/$spreadId/interpretations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )

        mockMvc
            .perform(
                post("/api/v0.0.1/spreads/$spreadId/interpretations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isConflict)
    }

    @Test
    fun `getInterpretations should return paginated interpretations`() {
        val spreadId = createSpread()
        val request = CreateInterpretationRequest(text = "Test interpretation", authorId = testUserId)

        mockMvc.perform(
            post("/api/v0.0.1/spreads/$spreadId/interpretations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )

        mockMvc
            .perform(get("/api/v0.0.1/spreads/$spreadId/interpretations?page=0&size=10"))
            .andExpect(status().isOk)
            .andExpect(header().exists("X-Total-Count"))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].text").value("Test interpretation"))
    }

    @Test
    fun `getInterpretation should return interpretation details`() {
        val spreadId = createSpread()
        val request = CreateInterpretationRequest(text = "Test interpretation", authorId = testUserId)

        val result =
            mockMvc
                .perform(
                    post("/api/v0.0.1/spreads/$spreadId/interpretations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andReturn()

        val interpretationId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

        mockMvc
            .perform(get("/api/v0.0.1/spreads/$spreadId/interpretations/$interpretationId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(interpretationId))
            .andExpect(jsonPath("$.text").value("Test interpretation"))
    }

    @Test
    fun `updateInterpretation should update interpretation when user is author`() {
        val spreadId = createSpread()
        val createRequest = CreateInterpretationRequest(text = "Original text", authorId = testUserId)

        val result =
            mockMvc
                .perform(
                    post("/api/v0.0.1/spreads/$spreadId/interpretations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)),
                ).andReturn()

        val interpretationId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

        val updateRequest = UpdateInterpretationRequest(text = "Updated text", authorId = testUserId)

        mockMvc
            .perform(
                put("/api/v0.0.1/spreads/$spreadId/interpretations/$interpretationId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.text").value("Updated text"))
    }

    @Test
    fun `updateInterpretation should return 403 when user is not author`() {
        val spreadId = createSpread()
        val createRequest = CreateInterpretationRequest(text = "Original text", authorId = testUserId)

        val result =
            mockMvc
                .perform(
                    post("/api/v0.0.1/spreads/$spreadId/interpretations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)),
                ).andReturn()

        val interpretationId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

        val updateRequest = UpdateInterpretationRequest(text = "Updated text", authorId = UUID.randomUUID())

        mockMvc
            .perform(
                put("/api/v0.0.1/spreads/$spreadId/interpretations/$interpretationId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `deleteInterpretation should delete interpretation when user is author`() {
        val spreadId = createSpread()
        val createRequest = CreateInterpretationRequest(text = "Test interpretation", authorId = testUserId)

        val result =
            mockMvc
                .perform(
                    post("/api/v0.0.1/spreads/$spreadId/interpretations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)),
                ).andReturn()

        val interpretationId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

        val deleteRequest = DeleteRequest(userId = testUserId)

        mockMvc
            .perform(
                delete("/api/v0.0.1/spreads/$spreadId/interpretations/$interpretationId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(deleteRequest)),
            ).andExpect(status().isNoContent)
    }

    @Test
    fun `deleteInterpretation should return 403 when user is not author`() {
        val spreadId = createSpread()
        val createRequest = CreateInterpretationRequest(text = "Test interpretation", authorId = testUserId)

        val result =
            mockMvc
                .perform(
                    post("/api/v0.0.1/spreads/$spreadId/interpretations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)),
                ).andReturn()

        val interpretationId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

        val deleteRequest = DeleteRequest(userId = UUID.randomUUID())

        mockMvc
            .perform(
                delete("/api/v0.0.1/spreads/$spreadId/interpretations/$interpretationId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(deleteRequest)),
            ).andExpect(status().isForbidden)
    }
}
