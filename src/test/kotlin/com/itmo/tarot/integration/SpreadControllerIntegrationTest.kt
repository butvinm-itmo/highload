package com.itmo.tarot.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.itmo.tarot.dto.request.CreateSpreadRequest
import com.itmo.tarot.dto.request.DeleteSpreadRequest
import com.itmo.tarot.entity.LayoutType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.hamcrest.Matchers

class SpreadControllerIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    lateinit var mockMvc: MockMvc
    
    @Autowired
    lateinit var objectMapper: ObjectMapper
    
    @Test
    fun `should create spread successfully`() {
        val request = CreateSpreadRequest(
            userId = 1L,
            question = "What does the future hold?",
            layoutType = LayoutType.THREE_CARDS
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
            .andExpect(jsonPath("$.cards").value(Matchers.hasSize<Any>(3)))
    }
    
    @Test
    fun `should fail validation for invalid create spread request`() {
        val request = CreateSpreadRequest(
            userId = 1L,
            question = "", // Invalid empty question
            layoutType = LayoutType.ONE_CARD
        )
        
        mockMvc.perform(post("/api/spreads")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation Failed"))
            .andExpect(jsonPath("$.validationErrors.question").exists())
    }
    
    @Test
    fun `should get spread by id`() {
        val createRequest = CreateSpreadRequest(
            userId = 2L,
            question = "Test question",
            layoutType = LayoutType.ONE_CARD
        )
        
        val createResult = mockMvc.perform(post("/api/spreads")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated)
            .andReturn()
        
        val responseContent = createResult.response.contentAsString
        val createdSpread = objectMapper.readTree(responseContent)
        val spreadId = createdSpread.get("id").asLong()
        
        mockMvc.perform(get("/api/spreads/$spreadId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(spreadId))
            .andExpect(jsonPath("$.question").value(createRequest.question))
    }
    
    @Test
    fun `should return 404 for non-existent spread`() {
        mockMvc.perform(get("/api/spreads/99999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Not Found"))
    }
    
    @Test
    fun `should get paginated spreads with total count header`() {
        mockMvc.perform(get("/api/spreads")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk)
            .andExpect(header().exists("X-Total-Count"))
            .andExpect(jsonPath("$").isArray)
    }
    
    @Test
    fun `should get spreads for infinite scroll`() {
        mockMvc.perform(get("/api/spreads/scroll")
            .param("size", "5"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }
    
    @Test
    fun `should delete spread successfully when authorized`() {
        val createRequest = CreateSpreadRequest(
            userId = 3L,
            question = "To be deleted",
            layoutType = LayoutType.CROSS
        )
        
        val createResult = mockMvc.perform(post("/api/spreads")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated)
            .andReturn()
        
        val responseContent = createResult.response.contentAsString
        val createdSpread = objectMapper.readTree(responseContent)
        val spreadId = createdSpread.get("id").asLong()
        
        val deleteRequest = DeleteSpreadRequest(userId = 3L)
        
        mockMvc.perform(delete("/api/spreads/$spreadId")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(deleteRequest)))
            .andExpect(status().isNoContent)
    }
    
    @Test
    fun `should fail to delete spread when unauthorized`() {
        val createRequest = CreateSpreadRequest(
            userId = 4L,
            question = "Cannot be deleted by others",
            layoutType = LayoutType.ONE_CARD
        )
        
        val createResult = mockMvc.perform(post("/api/spreads")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated)
            .andReturn()
        
        val responseContent = createResult.response.contentAsString
        val createdSpread = objectMapper.readTree(responseContent)
        val spreadId = createdSpread.get("id").asLong()
        
        val deleteRequest = DeleteSpreadRequest(userId = 999L) // Different user
        
        mockMvc.perform(delete("/api/spreads/$spreadId")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(deleteRequest)))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error").value("Forbidden"))
    }
}