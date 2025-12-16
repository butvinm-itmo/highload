package com.github.butvinmitmo.tarotservice.integration.controller

import org.junit.jupiter.api.Test

class CardControllerIntegrationTest : BaseControllerIntegrationTest() {
    @Test
    fun `GET cards should return paginated list with X-Total-Count header`() {
        webTestClient
            .get()
            .uri("/api/v0.0.1/cards")
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .exists("X-Total-Count")
            .expectBody()
            .jsonPath("$")
            .isArray
            .jsonPath("$[0].id")
            .exists()
            .jsonPath("$[0].name")
            .exists()
            .jsonPath("$[0].arcanaType")
            .exists()
    }

    @Test
    fun `GET cards with pagination should respect page and size parameters`() {
        webTestClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/api/v0.0.1/cards")
                    .queryParam("page", "0")
                    .queryParam("size", "5")
                    .build()
            }.exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.length()")
            .isEqualTo(5)
    }

    @Test
    fun `GET cards random should return requested number of cards`() {
        webTestClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/api/v0.0.1/cards/random")
                    .queryParam("count", "3")
                    .build()
            }.exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$")
            .isArray
            .jsonPath("$.length()")
            .isEqualTo(3)
            .jsonPath("$[0].id")
            .exists()
            .jsonPath("$[0].name")
            .exists()
            .jsonPath("$[0].arcanaType")
            .exists()
    }
}
