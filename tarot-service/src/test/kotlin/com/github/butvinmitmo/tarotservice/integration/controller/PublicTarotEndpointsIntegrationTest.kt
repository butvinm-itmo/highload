package com.github.butvinmitmo.tarotservice.integration.controller

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class PublicTarotEndpointsIntegrationTest : BaseControllerIntegrationTest() {
    @Test
    fun `GET layout-types by id should return layout type`() {
        val oneCardLayoutId = "30000000-0000-0000-0000-000000000001"

        mockMvc
            .perform(get("/api/v0.0.1/layout-types/$oneCardLayoutId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(oneCardLayoutId))
            .andExpect(jsonPath("$.name").value("ONE_CARD"))
            .andExpect(jsonPath("$.cardsCount").value(1))
    }

    @Test
    fun `GET layout-types by id should return 404 for non-existent id`() {
        val nonExistentId = UUID.randomUUID()

        mockMvc
            .perform(get("/api/v0.0.1/layout-types/$nonExistentId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET cards random should return requested number of cards`() {
        mockMvc
            .perform(
                get("/api/v0.0.1/cards/random")
                    .param("count", "3"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].name").exists())
            .andExpect(jsonPath("$[0].arcanaType").exists())
    }
}
