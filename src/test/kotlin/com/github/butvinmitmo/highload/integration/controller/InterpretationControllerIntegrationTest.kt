package com.github.butvinmitmo.highload.integration.controller

import com.github.butvinmitmo.highload.dto.CreateInterpretationRequest
import com.github.butvinmitmo.highload.dto.CreateSpreadRequest
import com.github.butvinmitmo.highload.dto.CreateUserRequest
import com.github.butvinmitmo.highload.dto.DeleteRequest
import com.github.butvinmitmo.highload.dto.UpdateInterpretationRequest
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class InterpretationControllerIntegrationTest : BaseControllerIntegrationTest() {
    private val spreadsUrl = "/api/v0.0.1/spreads"
    private val usersUrl = "/api/v0.0.1/users"

    private fun interpretationsUrl(spreadId: UUID) = "$spreadsUrl/$spreadId/interpretations"

    @Test
    fun `should create interpretation and return 201`() {
        val userId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("interpuser"), objectMapper)
                .getIdFromBody(objectMapper)

        val spreadId =
            mockMvc
                .postJson(
                    spreadsUrl,
                    CreateSpreadRequest("Question", oneCardLayoutId, userId),
                    objectMapper,
                ).getIdFromBody(objectMapper)

        val request =
            CreateInterpretationRequest(
                text = "This card represents new beginnings",
                authorId = userId,
            )

        val interpretationId =
            mockMvc
                .postJson(interpretationsUrl(spreadId), request, objectMapper)
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").exists())
                .getIdFromBody(objectMapper)

        // Verify interpretation can be retrieved with full details
        mockMvc
            .perform(get("${interpretationsUrl(spreadId)}/$interpretationId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(interpretationId.toString()))
            .andExpect(jsonPath("$.text").value("This card represents new beginnings"))
            .andExpect(jsonPath("$.author.id").value(userId.toString()))
    }

    @Test
    fun `should return 409 when creating duplicate interpretation`() {
        val userId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("dupuser"), objectMapper)
                .getIdFromBody(objectMapper)

        val spreadId =
            mockMvc
                .postJson(
                    spreadsUrl,
                    CreateSpreadRequest("Question", oneCardLayoutId, userId),
                    objectMapper,
                ).getIdFromBody(objectMapper)

        val request =
            CreateInterpretationRequest(
                text = "First interpretation",
                authorId = userId,
            )

        // Create first interpretation
        mockMvc
            .postJson(interpretationsUrl(spreadId), request, objectMapper)
            .andExpect(status().isCreated)

        // Attempt to create duplicate
        val duplicateRequest =
            CreateInterpretationRequest(
                text = "Second interpretation",
                authorId = userId,
            )

        mockMvc
            .postJson(interpretationsUrl(spreadId), duplicateRequest, objectMapper)
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `should list all interpretations for spread`() {
        val user1Id =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("user1"), objectMapper)
                .getIdFromBody(objectMapper)

        val user2Id =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("user2"), objectMapper)
                .getIdFromBody(objectMapper)

        val spreadId =
            mockMvc
                .postJson(
                    spreadsUrl,
                    CreateSpreadRequest("Question", oneCardLayoutId, user1Id),
                    objectMapper,
                ).getIdFromBody(objectMapper)

        // Create interpretations from different users
        mockMvc.postJson(
            interpretationsUrl(spreadId),
            CreateInterpretationRequest("Interpretation 1", user1Id),
            objectMapper,
        )
        mockMvc.postJson(
            interpretationsUrl(spreadId),
            CreateInterpretationRequest("Interpretation 2", user2Id),
            objectMapper,
        )

        mockMvc
            .perform(get(interpretationsUrl(spreadId)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].author.id").exists())
            .andExpect(jsonPath("$[1].author.id").exists())
    }

    @Test
    fun `should get interpretation by ID and return 200`() {
        val userId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("getuser"), objectMapper)
                .getIdFromBody(objectMapper)

        val spreadId =
            mockMvc
                .postJson(
                    spreadsUrl,
                    CreateSpreadRequest("Question", oneCardLayoutId, userId),
                    objectMapper,
                ).getIdFromBody(objectMapper)

        val interpretationId =
            mockMvc
                .postJson(
                    interpretationsUrl(spreadId),
                    CreateInterpretationRequest("Test text", userId),
                    objectMapper,
                ).getIdFromBody(objectMapper)

        mockMvc
            .perform(get("${interpretationsUrl(spreadId)}/$interpretationId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(interpretationId.toString()))
            .andExpect(jsonPath("$.text").value("Test text"))
    }

    @Test
    fun `should return 404 when getting non-existent interpretation`() {
        val userId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("someuser404"), objectMapper)
                .getIdFromBody(objectMapper)

        val spreadId =
            mockMvc
                .postJson(
                    spreadsUrl,
                    CreateSpreadRequest("Question", oneCardLayoutId, userId),
                    objectMapper,
                ).getIdFromBody(objectMapper)

        val nonExistentId = UUID.randomUUID()

        mockMvc
            .perform(get("${interpretationsUrl(spreadId)}/$nonExistentId"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `should update interpretation and return 200`() {
        val userId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("updateuser"), objectMapper)
                .getIdFromBody(objectMapper)

        val spreadId =
            mockMvc
                .postJson(
                    spreadsUrl,
                    CreateSpreadRequest("Question", oneCardLayoutId, userId),
                    objectMapper,
                ).getIdFromBody(objectMapper)

        val interpretationId =
            mockMvc
                .postJson(
                    interpretationsUrl(spreadId),
                    CreateInterpretationRequest("Old text", userId),
                    objectMapper,
                ).getIdFromBody(objectMapper)

        val updateRequest =
            UpdateInterpretationRequest(
                text = "Updated text",
                authorId = userId,
            )

        mockMvc
            .putJson("${interpretationsUrl(spreadId)}/$interpretationId", updateRequest, objectMapper)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.text").value("Updated text"))

        // Verify update persisted
        mockMvc
            .perform(get("${interpretationsUrl(spreadId)}/$interpretationId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.text").value("Updated text"))
    }

    @Test
    fun `should return 403 when updating interpretation by non-author`() {
        val authorId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("author2"), objectMapper)
                .getIdFromBody(objectMapper)

        val otherId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("other2"), objectMapper)
                .getIdFromBody(objectMapper)

        val spreadId =
            mockMvc
                .postJson(
                    spreadsUrl,
                    CreateSpreadRequest("Question", oneCardLayoutId, authorId),
                    objectMapper,
                ).getIdFromBody(objectMapper)

        val interpretationId =
            mockMvc
                .postJson(
                    interpretationsUrl(spreadId),
                    CreateInterpretationRequest("Original text", authorId),
                    objectMapper,
                ).getIdFromBody(objectMapper)

        // Attempt to update with different userId
        val updateRequest =
            UpdateInterpretationRequest(
                text = "Hacked text",
                authorId = otherId,
            )

        mockMvc
            .putJson("${interpretationsUrl(spreadId)}/$interpretationId", updateRequest, objectMapper)
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").exists())

        // Verify text unchanged
        mockMvc
            .perform(get("${interpretationsUrl(spreadId)}/$interpretationId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.text").value("Original text"))
    }

    @Test
    fun `should delete interpretation and return 204`() {
        val userId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("deleteuser2"), objectMapper)
                .getIdFromBody(objectMapper)

        val spreadId =
            mockMvc
                .postJson(
                    spreadsUrl,
                    CreateSpreadRequest("Question", oneCardLayoutId, userId),
                    objectMapper,
                ).getIdFromBody(objectMapper)

        val interpretationId =
            mockMvc
                .postJson(
                    interpretationsUrl(spreadId),
                    CreateInterpretationRequest("To be deleted", userId),
                    objectMapper,
                ).getIdFromBody(objectMapper)

        val deleteRequest = DeleteRequest(userId = userId)
        mockMvc
            .deleteJson("${interpretationsUrl(spreadId)}/$interpretationId", deleteRequest, objectMapper)
            .andExpect(status().isNoContent)

        // Verify interpretation is deleted
        mockMvc
            .perform(get("${interpretationsUrl(spreadId)}/$interpretationId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 403 when deleting interpretation by non-author`() {
        val authorId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("author3"), objectMapper)
                .getIdFromBody(objectMapper)

        val otherId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("other3"), objectMapper)
                .getIdFromBody(objectMapper)

        val spreadId =
            mockMvc
                .postJson(
                    spreadsUrl,
                    CreateSpreadRequest("Question", oneCardLayoutId, authorId),
                    objectMapper,
                ).getIdFromBody(objectMapper)

        val interpretationId =
            mockMvc
                .postJson(
                    interpretationsUrl(spreadId),
                    CreateInterpretationRequest("Protected", authorId),
                    objectMapper,
                ).getIdFromBody(objectMapper)

        // Attempt to delete with different userId
        val deleteRequest = DeleteRequest(userId = otherId)
        mockMvc
            .deleteJson("${interpretationsUrl(spreadId)}/$interpretationId", deleteRequest, objectMapper)
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").exists())

        // Verify interpretation still exists
        mockMvc
            .perform(get("${interpretationsUrl(spreadId)}/$interpretationId"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should return 404 when deleting non-existent interpretation`() {
        val userId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("someuser2"), objectMapper)
                .getIdFromBody(objectMapper)

        val spreadId =
            mockMvc
                .postJson(
                    spreadsUrl,
                    CreateSpreadRequest("Question", oneCardLayoutId, userId),
                    objectMapper,
                ).getIdFromBody(objectMapper)

        val nonExistentId = UUID.randomUUID()
        val deleteRequest = DeleteRequest(userId = userId)

        mockMvc
            .deleteJson("${interpretationsUrl(spreadId)}/$nonExistentId", deleteRequest, objectMapper)
            .andExpect(status().isNotFound)
    }
}
