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
    private fun setupMocks(role: String = "MEDIUM") {
        // Mock user service response
        val userDto =
            UserDto(
                id = testUserId,
                username = "admin",
                createdAt = Instant.parse("2024-01-01T00:00:00Z"),
                role = role,
            )
        `when`(userServiceClient.getUserById(testUserId, role, testUserId)).thenReturn(ResponseEntity.ok(userDto))

        // Mock layout type response
        val layoutTypeDto = LayoutTypeDto(id = oneCardLayoutId, name = "ONE_CARD", cardsCount = 1)
        `when`(
            tarotServiceClient.getLayoutTypeById(testUserId, role, oneCardLayoutId),
        ).thenReturn(ResponseEntity.ok(layoutTypeDto))

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
        `when`(tarotServiceClient.getRandomCards(testUserId, role, 1)).thenReturn(ResponseEntity.ok(cards))

        // Mock system context for mapper (used when fetching data)
        `when`(
            userServiceClient.getUserById(systemUserId, systemRole, testUserId),
        ).thenReturn(ResponseEntity.ok(userDto))
        `when`(
            tarotServiceClient.getLayoutTypeById(systemUserId, systemRole, oneCardLayoutId),
        ).thenReturn(ResponseEntity.ok(layoutTypeDto))
        `when`(tarotServiceClient.getRandomCards(systemUserId, systemRole, 1)).thenReturn(ResponseEntity.ok(cards))
    }

    private fun createSpread(role: String = "MEDIUM"): String {
        setupMocks(role)
        val request =
            CreateSpreadRequest(
                question = "Test question",
                layoutTypeId = oneCardLayoutId,
            )

        return webTestClient
            .post()
            .uri("/api/v0.0.1/spreads")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", role)
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
        val spreadId = createSpread("MEDIUM")
        val request = CreateInterpretationRequest(text = "Test interpretation")

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "MEDIUM")
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
        val spreadId = createSpread("MEDIUM")
        val request = CreateInterpretationRequest(text = "Test interpretation")

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "MEDIUM")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "MEDIUM")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isEqualTo(409)
    }

    @Test
    fun `getInterpretations should return paginated interpretations`() {
        val spreadId = createSpread("MEDIUM")
        val request = CreateInterpretationRequest(text = "Test interpretation")

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "MEDIUM")
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
        val spreadId = createSpread("MEDIUM")
        val request = CreateInterpretationRequest(text = "Test interpretation")

        val interpretationId =
            webTestClient
                .post()
                .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
                .header("X-User-Id", testUserId.toString())
                .header("X-User-Role", "MEDIUM")
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
        val spreadId = createSpread("MEDIUM")
        val createRequest = CreateInterpretationRequest(text = "Original text")

        val interpretationId =
            webTestClient
                .post()
                .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
                .header("X-User-Id", testUserId.toString())
                .header("X-User-Role", "MEDIUM")
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

        val updateRequest = UpdateInterpretationRequest(text = "Updated text")

        webTestClient
            .put()
            .uri("/api/v0.0.1/spreads/$spreadId/interpretations/$interpretationId")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "MEDIUM")
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
        val spreadId = createSpread("MEDIUM")
        val createRequest = CreateInterpretationRequest(text = "Original text")

        val interpretationId =
            webTestClient
                .post()
                .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
                .header("X-User-Id", testUserId.toString())
                .header("X-User-Role", "MEDIUM")
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

        val updateRequest = UpdateInterpretationRequest(text = "Updated text")

        webTestClient
            .put()
            .uri("/api/v0.0.1/spreads/$spreadId/interpretations/$interpretationId")
            .header("X-User-Id", UUID.randomUUID().toString())
            .header("X-User-Role", "MEDIUM")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `deleteInterpretation should delete interpretation when user is author`() {
        val spreadId = createSpread("MEDIUM")
        val createRequest = CreateInterpretationRequest(text = "Test interpretation")

        val interpretationId =
            webTestClient
                .post()
                .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
                .header("X-User-Id", testUserId.toString())
                .header("X-User-Role", "MEDIUM")
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
            .header("X-User-Role", "MEDIUM")
            .exchange()
            .expectStatus()
            .isNoContent
    }

    @Test
    fun `deleteInterpretation should return 403 when user is not author`() {
        val spreadId = createSpread("MEDIUM")
        val createRequest = CreateInterpretationRequest(text = "Test interpretation")

        val interpretationId =
            webTestClient
                .post()
                .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
                .header("X-User-Id", testUserId.toString())
                .header("X-User-Role", "MEDIUM")
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
            .header("X-User-Role", "MEDIUM")
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `addInterpretation should return 403 when user is USER role`() {
        val spreadId = createSpread("USER")
        val request = CreateInterpretationRequest(text = "Test interpretation")

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads/$spreadId/interpretations")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isForbidden
    }
}
