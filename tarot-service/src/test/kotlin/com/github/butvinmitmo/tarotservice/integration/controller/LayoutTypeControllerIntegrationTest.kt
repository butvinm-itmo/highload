package com.github.butvinmitmo.tarotservice.integration.controller

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class LayoutTypeControllerIntegrationTest : BaseControllerIntegrationTest() {
    @Test
    fun `GET layout-types should return paginated list with X-Total-Count header`() {
        mockMvc
            .perform(get("/api/v0.0.1/layout-types"))
            .andExpect(status().isOk)
            .andExpect(header().exists("X-Total-Count"))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].name").exists())
            .andExpect(jsonPath("$[0].cardsCount").exists())
    }
}
