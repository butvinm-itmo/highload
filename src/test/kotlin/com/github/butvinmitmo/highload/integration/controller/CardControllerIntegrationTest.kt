package com.github.butvinmitmo.highload.integration.controller

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class CardControllerIntegrationTest : BaseControllerIntegrationTest() {
    private val baseUrl = "/api/v0.0.1/cards"

    @Test
    fun `should return paginated list of cards with default parameters`() {
        mockMvc
            .perform(get(baseUrl))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].name").exists())
            .andExpect(jsonPath("$[0].arcanaType").exists())
            .andExpect(jsonPath("$[0].arcanaType.id").exists())
            .andExpect(jsonPath("$[0].arcanaType.name").exists())
    }

    @Test
    fun `should return paginated list with custom page and size`() {
        mockMvc
            .perform(get(baseUrl).param("page", "0").param("size", "5"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(5))
    }

    @Test
    fun `should return second page of cards`() {
        val firstPageResponse =
            mockMvc
                .perform(get(baseUrl).param("page", "0").param("size", "10"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString

        val firstPageCards = objectMapper.readTree(firstPageResponse)
        val firstPageFirstCardId = firstPageCards[0].get("id").asText()

        val secondPageResponse =
            mockMvc
                .perform(get(baseUrl).param("page", "1").param("size", "10"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString

        val secondPageCards = objectMapper.readTree(secondPageResponse)

        // Verify second page doesn't contain cards from first page
        if (secondPageCards.size() > 0) {
            val secondPageFirstCardId = secondPageCards[0].get("id").asText()
            assert(firstPageFirstCardId != secondPageFirstCardId)
        }
    }

    @Test
    fun `should return empty array when page exceeds available data`() {
        mockMvc
            .perform(get(baseUrl).param("page", "1000").param("size", "20"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `should validate card structure contains all required fields`() {
        val response =
            mockMvc
                .perform(get(baseUrl).param("page", "0").param("size", "1"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString

        val cards = objectMapper.readTree(response)
        val card = cards[0]

        // Verify card has all required fields
        assert(card.has("id"))
        assert(card.has("name"))
        assert(card.has("arcanaType"))

        // Verify arcanaType has required fields
        val arcanaType = card.get("arcanaType")
        assert(arcanaType.has("id"))
        assert(arcanaType.has("name"))
    }
}
