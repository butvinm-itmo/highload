package com.github.butvinmitmo.divinationservice.integration.controller

import com.github.butvinmitmo.divinationservice.entity.Interpretation
import com.github.butvinmitmo.shared.dto.ArcanaTypeDto
import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.LayoutTypeDto
import com.github.butvinmitmo.shared.dto.UserDto
import org.junit.jupiter.api.Test
import org.mockito.Mockito.lenient
import org.mockito.Mockito.`when`
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.UUID

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
class SpreadControllerIntegrationTest : BaseControllerIntegrationTest() {
    @Test
    fun `createSpread should create spread and return 201`() {
        // Mock user service response
        val userDto =
            UserDto(
                id = testUserId,
                username = "admin",
                createdAt = Instant.parse("2024-01-01T00:00:00Z"),
                role = "USER",
            )
        lenient()
            .`when`(
                userServiceClient.getUserById(testUserId, "USER", testUserId),
            ).thenReturn(ResponseEntity.ok(userDto))

        // Mock layout type response
        val layoutTypeDto = LayoutTypeDto(id = oneCardLayoutId, name = "ONE_CARD", cardsCount = 1)
        lenient()
            .`when`(
                tarotServiceClient.getLayoutTypeById(testUserId, "USER", oneCardLayoutId),
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
        lenient().`when`(tarotServiceClient.getRandomCards(testUserId, "USER", 1)).thenReturn(ResponseEntity.ok(cards))
        val request =
            CreateSpreadRequest(
                question = "Test question",
                layoutTypeId = oneCardLayoutId,
            )

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
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
                layoutTypeId = oneCardLayoutId,
            )

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
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
    fun `getSpread should return spread details`() {
        // Mock user service response for creating spread
        val userDto =
            UserDto(
                id = testUserId,
                username = "admin",
                createdAt = Instant.parse("2024-01-01T00:00:00Z"),
                role = "USER",
            )
        `when`(userServiceClient.getUserById(testUserId, "USER", testUserId)).thenReturn(ResponseEntity.ok(userDto))

        // Mock layout type response for creating spread
        val layoutTypeDto = LayoutTypeDto(id = oneCardLayoutId, name = "ONE_CARD", cardsCount = 1)
        `when`(
            tarotServiceClient.getLayoutTypeById(testUserId, "USER", oneCardLayoutId),
        ).thenReturn(ResponseEntity.ok(layoutTypeDto))

        // Mock random cards response for creating spread
        val arcanaType = ArcanaTypeDto(id = UUID.fromString("00000000-0000-0000-0000-000000000010"), name = "MAJOR")
        val cards =
            listOf(
                CardDto(
                    id = UUID.fromString("00000000-0000-0000-0000-000000000030"),
                    name = "The Fool",
                    arcanaType = arcanaType,
                ),
            )
        `when`(tarotServiceClient.getRandomCards(testUserId, "USER", 1)).thenReturn(ResponseEntity.ok(cards))

        // Mock system context for mapper (used when fetching spread details)
        `when`(
            userServiceClient.getUserById(systemUserId, systemRole, testUserId),
        ).thenReturn(ResponseEntity.ok(userDto))
        `when`(
            tarotServiceClient.getLayoutTypeById(systemUserId, systemRole, oneCardLayoutId),
        ).thenReturn(ResponseEntity.ok(layoutTypeDto))
        `when`(tarotServiceClient.getCards(systemUserId, systemRole, 0, 50)).thenReturn(ResponseEntity.ok(cards))
        val request =
            CreateSpreadRequest(
                question = "Test question",
                layoutTypeId = oneCardLayoutId,
            )

        val spreadId =
            webTestClient
                .post()
                .uri("/api/v0.0.1/spreads")
                .header("X-User-Id", testUserId.toString())
                .header("X-User-Role", "USER")
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
    fun `deleteSpread should delete spread when user is author`() {
        // Mock user service response
        val userDto =
            UserDto(
                id = testUserId,
                username = "admin",
                createdAt = Instant.parse("2024-01-01T00:00:00Z"),
                role = "USER",
            )
        `when`(userServiceClient.getUserById(testUserId, "USER", testUserId)).thenReturn(ResponseEntity.ok(userDto))

        // Mock layout type response
        val layoutTypeDto = LayoutTypeDto(id = oneCardLayoutId, name = "ONE_CARD", cardsCount = 1)
        `when`(
            tarotServiceClient.getLayoutTypeById(testUserId, "USER", oneCardLayoutId),
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
        `when`(tarotServiceClient.getRandomCards(testUserId, "USER", 1)).thenReturn(ResponseEntity.ok(cards))
        val request =
            CreateSpreadRequest(
                question = "Test question",
                layoutTypeId = oneCardLayoutId,
            )

        val spreadId =
            webTestClient
                .post()
                .uri("/api/v0.0.1/spreads")
                .header("X-User-Id", testUserId.toString())
                .header("X-User-Role", "USER")
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
            .method(org.springframework.http.HttpMethod.DELETE)
            .uri("/api/v0.0.1/spreads/$spreadId")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
            .header("X-User-Role", "USER")
            .exchange()
            .expectStatus()
            .isNoContent
    }

    @Test
    fun `deleteSpread should return 403 when user is not author`() {
        // Mock user service response
        val userDto =
            UserDto(
                id = testUserId,
                username = "admin",
                createdAt = Instant.parse("2024-01-01T00:00:00Z"),
                role = "USER",
            )
        `when`(userServiceClient.getUserById(testUserId, "USER", testUserId)).thenReturn(ResponseEntity.ok(userDto))

        // Mock layout type response
        val layoutTypeDto = LayoutTypeDto(id = oneCardLayoutId, name = "ONE_CARD", cardsCount = 1)
        `when`(
            tarotServiceClient.getLayoutTypeById(testUserId, "USER", oneCardLayoutId),
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
        `when`(tarotServiceClient.getRandomCards(testUserId, "USER", 1)).thenReturn(ResponseEntity.ok(cards))
        val request =
            CreateSpreadRequest(
                question = "Test question",
                layoutTypeId = oneCardLayoutId,
            )

        val spreadId =
            webTestClient
                .post()
                .uri("/api/v0.0.1/spreads")
                .header("X-User-Id", testUserId.toString())
                .header("X-User-Role", "USER")
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

        val nonAuthorUserId = UUID.randomUUID()

        webTestClient
            .method(org.springframework.http.HttpMethod.DELETE)
            .uri("/api/v0.0.1/spreads/$spreadId")
            .header("X-User-Id", nonAuthorUserId.toString())
            .header("X-User-Role", "USER")
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `getSpreadsByScroll should return spreads with cursor`() {
        val request =
            CreateSpreadRequest(
                question = "Test question",
                layoutTypeId = oneCardLayoutId,
            )

        webTestClient
            .post()
            .uri("/api/v0.0.1/spreads")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
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

    @Test
    fun `getSpread should return interpretations with fileUrl when file is attached`() {
        // Mock user service response for creating spread
        val userDto =
            UserDto(
                id = testUserId,
                username = "admin",
                createdAt = Instant.parse("2024-01-01T00:00:00Z"),
                role = "MEDIUM",
            )
        `when`(userServiceClient.getUserById(testUserId, "MEDIUM", testUserId)).thenReturn(ResponseEntity.ok(userDto))

        // Mock layout type response for creating spread
        val layoutTypeDto = LayoutTypeDto(id = oneCardLayoutId, name = "ONE_CARD", cardsCount = 1)
        `when`(
            tarotServiceClient.getLayoutTypeById(testUserId, "MEDIUM", oneCardLayoutId),
        ).thenReturn(ResponseEntity.ok(layoutTypeDto))

        // Mock random cards response for creating spread
        val arcanaType = ArcanaTypeDto(id = UUID.fromString("00000000-0000-0000-0000-000000000010"), name = "MAJOR")
        val cards =
            listOf(
                CardDto(
                    id = UUID.fromString("00000000-0000-0000-0000-000000000030"),
                    name = "The Fool",
                    arcanaType = arcanaType,
                ),
            )
        `when`(tarotServiceClient.getRandomCards(testUserId, "MEDIUM", 1)).thenReturn(ResponseEntity.ok(cards))

        // Mock system context for mapper (used when fetching spread details)
        `when`(
            userServiceClient.getUserById(systemUserId, systemRole, testUserId),
        ).thenReturn(ResponseEntity.ok(userDto))
        `when`(
            tarotServiceClient.getLayoutTypeById(systemUserId, systemRole, oneCardLayoutId),
        ).thenReturn(ResponseEntity.ok(layoutTypeDto))
        `when`(tarotServiceClient.getCards(systemUserId, systemRole, 0, 50)).thenReturn(ResponseEntity.ok(cards))

        // Create spread
        val request =
            CreateSpreadRequest(
                question = "Test question with file",
                layoutTypeId = oneCardLayoutId,
            )

        val spreadId =
            webTestClient
                .post()
                .uri("/api/v0.0.1/spreads")
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

        // Create interpretation with fileKey directly in database
        val fileKey = "interpretations/test-interpretation-id/test-image.png"
        val interpretation =
            Interpretation(
                text = "Test interpretation with file",
                authorId = testUserId,
                spreadId = UUID.fromString(spreadId),
                fileKey = fileKey,
            )
        interpretationRepository.save(interpretation).block()

        // Verify spread details include fileUrl for interpretation
        webTestClient
            .get()
            .uri("/api/v0.0.1/spreads/$spreadId")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.id")
            .isEqualTo(spreadId)
            .jsonPath("$.interpretations[0].fileUrl")
            .exists()
            .jsonPath("$.interpretations[0].fileUrl")
            .value<String> { url ->
                assert(url.contains(fileKey)) { "fileUrl should contain the fileKey: $url" }
            }
    }
}
