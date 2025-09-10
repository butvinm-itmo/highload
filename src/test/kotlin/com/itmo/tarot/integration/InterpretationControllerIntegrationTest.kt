package com.itmo.tarot.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.itmo.tarot.dto.request.CreateInterpretationRequest
import com.itmo.tarot.dto.request.CreateSpreadRequest
import com.itmo.tarot.dto.request.DeleteInterpretationRequest
import com.itmo.tarot.dto.request.UpdateInterpretationRequest
import com.itmo.tarot.entity.LayoutType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class InterpretationControllerIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    lateinit var mockMvc: MockMvc
    
    @Autowired
    lateinit var objectMapper: ObjectMapper
    
    private var testSpreadId: Long = 0
    
    @BeforeEach
    fun setupTestSpread() {
        val createSpreadRequest = CreateSpreadRequest(
            userId = 100L,
            question = "Test spread for interpretations",
            layoutType = LayoutType.THREE_CARDS
        )
        
        val createResult = mockMvc.perform(post("/api/spreads")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createSpreadRequest)))
            .andExpect(status().isCreated)
            .andReturn()
        
        val responseContent = createResult.response.contentAsString
        val createdSpread = objectMapper.readTree(responseContent)
        testSpreadId = createdSpread.get("id").asLong()
    }
    
    @Test
    fun `should create interpretation successfully`() {
        val request = CreateInterpretationRequest(
            userId = 200L,
            text = "This is a meaningful interpretation of the cards"
        )
        
        mockMvc.perform(post("/api/spreads/$testSpreadId/interpretations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.text").value(request.text))
            .andExpect(jsonPath("$.authorId").value(request.userId))
            .andExpect(jsonPath("$.createdAt").exists())
    }
    
    @Test
    fun `should fail to create duplicate interpretation for same user and spread`() {
        val request = CreateInterpretationRequest(
            userId = 201L,
            text = "First interpretation"
        )
        
        // Create first interpretation
        mockMvc.perform(post("/api/spreads/$testSpreadId/interpretations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
        
        // Attempt to create second interpretation for same user and spread
        val duplicateRequest = CreateInterpretationRequest(
            userId = 201L,
            text = "Duplicate interpretation"
        )
        
        mockMvc.perform(post("/api/spreads/$testSpreadId/interpretations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(duplicateRequest)))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("Conflict"))
    }
    
    @Test
    fun `should update interpretation when authorized`() {
        val createRequest = CreateInterpretationRequest(
            userId = 202L,
            text = "Original interpretation"
        )
        
        val createResult = mockMvc.perform(post("/api/spreads/$testSpreadId/interpretations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated)
            .andReturn()
        
        val responseContent = createResult.response.contentAsString
        val createdInterpretation = objectMapper.readTree(responseContent)
        val interpretationId = createdInterpretation.get("id").asLong()
        
        val updateRequest = UpdateInterpretationRequest(
            userId = 202L,
            text = "Updated interpretation text"
        )
        
        mockMvc.perform(put("/api/spreads/$testSpreadId/interpretations/$interpretationId")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.text").value(updateRequest.text))
            .andExpect(jsonPath("$.authorId").value(updateRequest.userId))
    }
    
    @Test
    fun `should fail to update interpretation when unauthorized`() {
        val createRequest = CreateInterpretationRequest(
            userId = 203L,
            text = "Protected interpretation"
        )
        
        val createResult = mockMvc.perform(post("/api/spreads/$testSpreadId/interpretations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated)
            .andReturn()
        
        val responseContent = createResult.response.contentAsString
        val createdInterpretation = objectMapper.readTree(responseContent)
        val interpretationId = createdInterpretation.get("id").asLong()
        
        val updateRequest = UpdateInterpretationRequest(
            userId = 999L, // Different user
            text = "Unauthorized update"
        )
        
        mockMvc.perform(put("/api/spreads/$testSpreadId/interpretations/$interpretationId")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error").value("Forbidden"))
    }
    
    @Test
    fun `should delete interpretation when authorized`() {
        val createRequest = CreateInterpretationRequest(
            userId = 204L,
            text = "To be deleted"
        )
        
        val createResult = mockMvc.perform(post("/api/spreads/$testSpreadId/interpretations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated)
            .andReturn()
        
        val responseContent = createResult.response.contentAsString
        val createdInterpretation = objectMapper.readTree(responseContent)
        val interpretationId = createdInterpretation.get("id").asLong()
        
        val deleteRequest = DeleteInterpretationRequest(userId = 204L)
        
        mockMvc.perform(delete("/api/spreads/$testSpreadId/interpretations/$interpretationId")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(deleteRequest)))
            .andExpect(status().isNoContent)
    }
    
    @Test
    fun `should get all interpretations for spread`() {
        // Create multiple interpretations
        val users = listOf(205L, 206L, 207L)
        
        users.forEach { userId ->
            val request = CreateInterpretationRequest(
                userId = userId,
                text = "Interpretation by user $userId"
            )
            
            mockMvc.perform(post("/api/spreads/$testSpreadId/interpretations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated)
        }
        
        mockMvc.perform(get("/api/spreads/$testSpreadId/interpretations"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(users.size))
    }
}