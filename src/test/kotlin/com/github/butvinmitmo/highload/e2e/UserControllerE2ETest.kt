package com.github.butvinmitmo.highload.e2e

import com.github.butvinmitmo.highload.dto.CreateUserRequest
import com.github.butvinmitmo.highload.dto.UpdateUserRequest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class UserControllerE2ETest : BaseE2ETest() {
    private val baseUrl = "/api/v0.0.1/users"

    @Test
    fun `should create user and return 201 with ID`() {
        val request = CreateUserRequest(username = "testuser")

        val userId =
            mockMvc
                .postJson(baseUrl, request, objectMapper)
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").exists())
                .getIdFromBody(objectMapper)

        // Verify user can be retrieved
        mockMvc
            .perform(get("$baseUrl/$userId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("testuser"))
    }

    @Test
    fun `should return 409 when creating user with duplicate username`() {
        val request = CreateUserRequest(username = "duplicate")

        // Create first user
        mockMvc
            .postJson(baseUrl, request, objectMapper)
            .andExpect(status().isCreated)

        // Attempt to create duplicate
        mockMvc
            .postJson(baseUrl, request, objectMapper)
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `should return 400 when creating user with invalid request`() {
        val invalidJson = """{"username": ""}"""

        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `should get user by ID and return 200`() {
        val request = CreateUserRequest(username = "gettest")
        val userId =
            mockMvc
                .postJson(baseUrl, request, objectMapper)
                .andExpect(status().isCreated)
                .getIdFromBody(objectMapper)

        mockMvc
            .perform(get("$baseUrl/$userId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(userId.toString()))
            .andExpect(jsonPath("$.username").value("gettest"))
    }

    @Test
    fun `should return 404 when getting non-existent user`() {
        val nonExistentId = UUID.randomUUID()

        mockMvc
            .perform(get("$baseUrl/$nonExistentId"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `should return paginated user list with X-Total-Count header`() {
        // Create multiple users
        for (i in 1..5) {
            mockMvc.postJson(baseUrl, CreateUserRequest("user$i"), objectMapper)
        }

        mockMvc
            .perform(get(baseUrl).param("page", "0").param("size", "3"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].username").exists())
    }

    @Test
    fun `should return empty list when requesting page beyond available data`() {
        mockMvc
            .perform(get(baseUrl).param("page", "999").param("size", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    fun `should update user and return 200`() {
        val createRequest = CreateUserRequest(username = "oldname")
        val userId =
            mockMvc
                .postJson(baseUrl, createRequest, objectMapper)
                .getIdFromBody(objectMapper)

        val updateRequest = UpdateUserRequest(username = "newname")
        mockMvc
            .putJson("$baseUrl/$userId", updateRequest, objectMapper)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("newname"))

        // Verify update persisted
        mockMvc
            .perform(get("$baseUrl/$userId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("newname"))
    }

    @Test
    fun `should return 404 when updating non-existent user`() {
        val nonExistentId = UUID.randomUUID()
        val updateRequest = UpdateUserRequest(username = "newname")

        mockMvc
            .putJson("$baseUrl/$nonExistentId", updateRequest, objectMapper)
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should delete user and return 204`() {
        val request = CreateUserRequest(username = "todelete")
        val userId =
            mockMvc
                .postJson(baseUrl, request, objectMapper)
                .getIdFromBody(objectMapper)

        mockMvc
            .perform(delete("$baseUrl/$userId"))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))

        // Verify user is deleted
        mockMvc
            .perform(get("$baseUrl/$userId"))
            .andExpect(status().isNotFound)
    }
}
