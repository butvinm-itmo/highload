package com.github.butvinmitmo.userservice.integration.controller

import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.UpdateUserRequest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class UserControllerIntegrationTest : BaseControllerIntegrationTest() {
    private val baseUrl = "/api/v0.0.1/users"

    @Test
    fun `POST users should create user and return 201`() {
        val request = CreateUserRequest(username = "newuser")

        mockMvc
            .perform(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
    }

    @Test
    fun `POST users should return 409 for duplicate username`() {
        val request = CreateUserRequest(username = "duplicateuser")

        mockMvc
            .perform(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)

        mockMvc
            .perform(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isConflict)
    }

    @Test
    fun `GET users should return paginated list with X-Total-Count header`() {
        val request = CreateUserRequest(username = "listuser")
        mockMvc
            .perform(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )

        mockMvc
            .perform(get(baseUrl))
            .andExpect(status().isOk)
            .andExpect(header().exists("X-Total-Count"))
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `GET users by id should return user`() {
        val createRequest = CreateUserRequest(username = "getbyiduser")
        val createResult =
            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)),
                ).andExpect(status().isCreated)
                .andReturn()

        val responseJson = createResult.response.contentAsString
        val userId = objectMapper.readTree(responseJson).get("id").asText()

        mockMvc
            .perform(get("$baseUrl/$userId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.username").value("getbyiduser"))
    }

    @Test
    fun `GET users by id should return 404 for non-existent user`() {
        val nonExistentId = UUID.randomUUID()

        mockMvc
            .perform(get("$baseUrl/$nonExistentId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT users should update user`() {
        val createRequest = CreateUserRequest(username = "originaluser")
        val createResult =
            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)),
                ).andExpect(status().isCreated)
                .andReturn()

        val responseJson = createResult.response.contentAsString
        val userId = objectMapper.readTree(responseJson).get("id").asText()

        val updateRequest = UpdateUserRequest(username = "updateduser")
        mockMvc
            .perform(
                put("$baseUrl/$userId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("updateduser"))
    }

    @Test
    fun `DELETE users should delete user and return 204`() {
        val createRequest = CreateUserRequest(username = "todelete")
        val createResult =
            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)),
                ).andExpect(status().isCreated)
                .andReturn()

        val responseJson = createResult.response.contentAsString
        val userId = objectMapper.readTree(responseJson).get("id").asText()

        mockMvc
            .perform(delete("$baseUrl/$userId"))
            .andExpect(status().isNoContent)

        mockMvc
            .perform(get("$baseUrl/$userId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE users should return 404 for non-existent user`() {
        val nonExistentId = UUID.randomUUID()

        mockMvc
            .perform(delete("$baseUrl/$nonExistentId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET internal users endpoint should return user`() {
        val createRequest = CreateUserRequest(username = "internaluser")
        val createResult =
            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)),
                ).andExpect(status().isCreated)
                .andReturn()

        val responseJson = createResult.response.contentAsString
        val userId = objectMapper.readTree(responseJson).get("id").asText()

        mockMvc
            .perform(get("/api/internal/users/$userId/entity"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.username").value("internaluser"))
    }
}
