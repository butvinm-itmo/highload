package com.github.butvinmitmo.highload.integration.controller

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class LayoutTypeControllerIntegrationTest : BaseControllerIntegrationTest() {
    private val baseUrl = "/api/v0.0.1/layout-types"

    @Test
    fun `should return list of layout types with default parameters`() {
        mockMvc
            .perform(get(baseUrl))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].name").exists())
            .andExpect(jsonPath("$[0].cardsCount").exists())
    }

    @Test
    fun `should return all three layout types`() {
        val response =
            mockMvc
                .perform(get(baseUrl).param("page", "0").param("size", "10"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString

        val layoutTypes = objectMapper.readTree(response)

        // Verify we have exactly 3 layout types
        assert(layoutTypes.size() == 3)

        // Collect all layout type names
        val names = layoutTypes.map { it.get("name").asText() }.toSet()

        // Verify all expected layout types are present
        assert(names.contains("ONE_CARD"))
        assert(names.contains("THREE_CARDS"))
        assert(names.contains("CROSS"))
    }

    @Test
    fun `should return correct cards count for each layout type`() {
        val response =
            mockMvc
                .perform(get(baseUrl))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString

        val layoutTypes = objectMapper.readTree(response)

        for (layoutType in layoutTypes) {
            val name = layoutType.get("name").asText()
            val cardsCount = layoutType.get("cardsCount").asInt()

            when (name) {
                "ONE_CARD" -> assert(cardsCount == 1)
                "THREE_CARDS" -> assert(cardsCount == 3)
                "CROSS" -> assert(cardsCount == 10)
            }
        }
    }

    @Test
    fun `should return paginated list with custom size`() {
        mockMvc
            .perform(get(baseUrl).param("page", "0").param("size", "2"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `should return empty array when page exceeds available data`() {
        mockMvc
            .perform(get(baseUrl).param("page", "10").param("size", "20"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `should validate layout type structure contains all required fields`() {
        val response =
            mockMvc
                .perform(get(baseUrl).param("page", "0").param("size", "1"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString

        val layoutTypes = objectMapper.readTree(response)
        val layoutType = layoutTypes[0]

        // Verify layout type has all required fields
        assert(layoutType.has("id"))
        assert(layoutType.has("name"))
        assert(layoutType.has("cardsCount"))

        // Verify field types
        assert(layoutType.get("id").isTextual)
        assert(layoutType.get("name").isTextual)
        assert(layoutType.get("cardsCount").isInt)
    }

    @Test
    fun `should verify layout type IDs match expected values`() {
        val response =
            mockMvc
                .perform(get(baseUrl))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString

        val layoutTypes = objectMapper.readTree(response)

        // Collect all IDs
        val ids = layoutTypes.map { it.get("id").asText() }

        // Verify the predefined layout type IDs are in the response
        assert(ids.contains(oneCardLayoutId.toString()))
        assert(ids.contains(threeCardsLayoutId.toString()))
        assert(ids.contains(crossLayoutId.toString()))
    }
}
