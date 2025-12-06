package com.github.butvinmitmo.tarotservice.integration.controller

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class CardControllerIntegrationTest : BaseControllerIntegrationTest() {
    @Test
    fun `GET cards should return paginated list with X-Total-Count header`() {
        mockMvc
            .perform(get("/api/v0.0.1/cards"))
            .andExpect(status().isOk)
            .andExpect(header().exists("X-Total-Count"))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].name").exists())
            .andExpect(jsonPath("$[0].arcanaType").exists())
    }

    @Test
    fun `GET cards with pagination should respect page and size parameters`() {
        mockMvc
            .perform(
                get("/api/v0.0.1/cards")
                    .param("page", "0")
                    .param("size", "5"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(5))
    }
}
