package com.github.butvinmitmo.tarotservice.integration.controller

import org.junit.jupiter.api.Test
import java.util.UUID

class LayoutTypeControllerIntegrationTest : BaseControllerIntegrationTest() {
    private val testUserId = UUID.randomUUID()

    @Test
    fun `GET layout-types should return paginated list with X-Total-Count header`() {
        webTestClient
            .get()
            .uri("/api/v0.0.1/layout-types")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .exists("X-Total-Count")
            .expectBody()
            .jsonPath("$")
            .isArray
            .jsonPath("$.length()")
            .isEqualTo(3)
            .jsonPath("$[0].id")
            .exists()
            .jsonPath("$[0].name")
            .exists()
            .jsonPath("$[0].cardsCount")
            .exists()
    }

    @Test
    fun `GET layout-types by id should return layout type`() {
        val oneCardLayoutId = "30000000-0000-0000-0000-000000000001"

        webTestClient
            .get()
            .uri("/api/v0.0.1/layout-types/$oneCardLayoutId")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.id")
            .isEqualTo(oneCardLayoutId)
            .jsonPath("$.name")
            .isEqualTo("ONE_CARD")
            .jsonPath("$.cardsCount")
            .isEqualTo(1)
    }

    @Test
    fun `GET layout-types by id should return 404 for non-existent id`() {
        val nonExistentId = UUID.randomUUID()

        webTestClient
            .get()
            .uri("/api/v0.0.1/layout-types/$nonExistentId")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
            .exchange()
            .expectStatus()
            .isNotFound
    }
}
