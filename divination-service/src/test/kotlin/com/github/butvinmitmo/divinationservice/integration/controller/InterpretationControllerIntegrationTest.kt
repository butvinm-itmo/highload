package com.github.butvinmitmo.divinationservice.integration.controller

import com.github.butvinmitmo.shared.dto.ArcanaTypeDto
import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.CreateInterpretationRequest
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.LayoutTypeDto
import com.github.butvinmitmo.shared.dto.UpdateInterpretationRequest
import com.github.butvinmitmo.shared.dto.UserDto
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.UUID

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
class InterpretationControllerIntegrationTest : BaseControllerIntegrationTest() {
    private fun setupMocks() {
        // Mock user service response
        val userDto =
            UserDto(
                id = testUserId,
                username = "admin",
                createdAt = Instant.parse("2024-01-01T00:00:00Z"),
                role = "USER",
            )
        `when`(userServiceClient.getUserById(testUserId)).thenReturn(ResponseEntity.ok(userDto))

        // Mock layout type response
        val layoutTypeDto = LayoutTypeDto(id = oneCardLayoutId, name = "ONE_CARD", cardsCount = 1)
        `when`(tarotServiceClient.getLayoutTypeById(oneCardLayoutId)).thenReturn(ResponseEntity.ok(layoutTypeDto))

        // Mock random cards response
        val arcanaType = ArcanaTypeDto(id = UUID.fromString("00000000-0000-0000-0000-000000000010"), name = "MAJOR")
        val cards =
            listOf(
                CardDto(
                    id = UUID.fromString("00000000-0000-0000-0000-000000000030"),
                    name = "The Fool",
                    arcanaType = arcanaType,
                ),
            )
        `when`(tarotServiceClient.getRandomCards(1)).thenReturn(ResponseEntity.ok(cards))
    }

    private fun createSpread(): String {
        setupMocks()
        val request =
            CreateSpreadRequest(
                question = "Test question",
                authorId = testUserId,
                layoutTypeId = oneCardLayoutId,
            )

        return webTestClient
            .post()
            .uri("/api/v0.0.1/spreads")
            .header("X-User-Id", testUserId.toString())
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
    }

    @Test
    fun `addInterpretation should create interpretation and return 201`() {
        val spreadId = createSpread()
        val request = CreateInterpretationRequest(text = "Test interpretation", authorId = testUserId)

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
            .header("X-User-Id", testUserId.toString())
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
    fun `addInterpretation should return 409 when user already has interpretation`() {
        val spreadId = createSpread()
        val request = CreateInterpretationRequest(text = "Test interpretation", authorId = testUserId)

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
            .header("X-User-Id", testUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
            .header("X-User-Id", testUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isEqualTo(409)
    }

    @Test
    fun `getInterpretations should return paginated interpretations`() {
        val spreadId = createSpread()
        val request = CreateInterpretationRequest(text = "Test interpretation", authorId = testUserId)

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
            .header("X-User-Id", testUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()

        webTestClient
            .get()
            .uri("/api/v0.0.1/spreads/$spreadId/interpretations?page=0&size=10")
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .exists("X-Total-Count")
            .expectBody()
            .jsonPath("$")
            .isArray
            .jsonPath("$[0].text")
            .isEqualTo("Test interpretation")
    }

    @Test
    fun `getInterpretation should return interpretation details`() {
        val spreadId = createSpread()
        val request = CreateInterpretationRequest(text = "Test interpretation", authorId = testUserId)

        val interpretationId =
            webTestClient
                .post()
                .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
                .header("X-User-Id", testUserId.toString())
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
            .uri("/api/v0.0.1/spreads/$spreadId/interpretations/$interpretationId")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.id")
            .isEqualTo(interpretationId)
            .jsonPath("$.text")
            .isEqualTo("Test interpretation")
    }

    @Test
    fun `updateInterpretation should update interpretation when user is author`() {
        val spreadId = createSpread()
        val createRequest = CreateInterpretationRequest(text = "Original text", authorId = testUserId)

        val interpretationId =
            webTestClient
                .post()
                .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
                .header("X-User-Id", testUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
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

        val updateRequest = UpdateInterpretationRequest(text = "Updated text", authorId = testUserId)

        webTestClient
            .put()
            .uri("/api/v0.0.1/spreads/$spreadId/interpretations/$interpretationId")
            .header("X-User-Id", updateRequest.authorId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.text")
            .isEqualTo("Updated text")
    }

    @Test
    fun `updateInterpretation should return 403 when user is not author`() {
        val spreadId = createSpread()
        val createRequest = CreateInterpretationRequest(text = "Original text", authorId = testUserId)

        val interpretationId =
            webTestClient
                .post()
                .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
                .header("X-User-Id", testUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
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

        val updateRequest = UpdateInterpretationRequest(text = "Updated text", authorId = UUID.randomUUID())

        webTestClient
            .put()
            .uri("/api/v0.0.1/spreads/$spreadId/interpretations/$interpretationId")
            .header("X-User-Id", updateRequest.authorId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `deleteInterpretation should delete interpretation when user is author`() {
        val spreadId = createSpread()
        val createRequest = CreateInterpretationRequest(text = "Test interpretation", authorId = testUserId)

        val interpretationId =
            webTestClient
                .post()
                .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
                .header("X-User-Id", testUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
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
            .method(org.springframework.http.HttpMethod.DELETE)
            .uri("/api/v0.0.1/spreads/$spreadId/interpretations/$interpretationId")
            .header("X-User-Id", testUserId.toString())
            .exchange()
            .expectStatus()
            .isNoContent
    }

    @Test
    fun `deleteInterpretation should return 403 when user is not author`() {
        val spreadId = createSpread()
        val createRequest = CreateInterpretationRequest(text = "Test interpretation", authorId = testUserId)

        val interpretationId =
            webTestClient
                .post()
                .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
                .header("X-User-Id", testUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
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

        val differentUserId = UUID.randomUUID()

        webTestClient
            .method(org.springframework.http.HttpMethod.DELETE)
            .uri("/api/v0.0.1/spreads/$spreadId/interpretations/$interpretationId")
            .header("X-User-Id", differentUserId.toString())
            .exchange()
            .expectStatus()
            .isForbidden
    }
}
