package com.itmo.tarot.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.itmo.tarot.dto.request.CreateSpreadRequest
import com.itmo.tarot.entity.LayoutType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class SimpleIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    lateinit var mockMvc: MockMvc
    
    @Autowired
    lateinit var objectMapper: ObjectMapper
    
    @Test
    fun `application context loads successfully`() {
        // Simple test to verify Spring context loads without errors
    }
    
    @Test
    fun `should create spread successfully`() {
        val request = CreateSpreadRequest(
            userId = 1L,
            question = "Test question for integration",
            layoutType = LayoutType.ONE_CARD
        )
        
        mockMvc.perform(post("/api/spreads")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.question").value(request.question))
            .andExpect(jsonPath("$.layoutType").value(request.layoutType.toString()))
            .andExpect(jsonPath("$.authorId").value(request.userId))
            .andExpect(jsonPath("$.cards").isArray)
    }
    
    @Test
    fun `should get spreads with pagination`() {
        mockMvc.perform(get("/api/spreads")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk)
            .andExpect(header().exists("X-Total-Count"))
            .andExpect(jsonPath("$").isArray)
    }
}