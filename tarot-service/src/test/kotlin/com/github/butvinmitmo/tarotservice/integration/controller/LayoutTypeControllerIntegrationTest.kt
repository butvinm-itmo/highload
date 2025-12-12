package com.github.butvinmitmo.tarotservice.integration.controller

import org.junit.jupiter.api.Test

class LayoutTypeControllerIntegrationTest : BaseControllerIntegrationTest() {
    @Test
    fun `GET layout-types should return paginated list with X-Total-Count header`() {
        webTestClient
            .get()
            .uri("/api/v0.0.1/layout-types")
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
}
