package com.github.butvinmitmo.userservice.integration.controller

import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.LoginRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class AuthControllerIntegrationTest : BaseControllerIntegrationTest() {
    private val authUrl = "/api/v0.0.1/auth/login"
    private val usersUrl = "/api/v0.0.1/users"
    private val testAdminUserId = UUID.fromString("10000000-0000-0000-0000-000000000001")

    @BeforeEach
    fun setup() {
        // Create a test user for authentication tests
        val createRequest = CreateUserRequest(username = "authuser", password = "Test@123")
        mockMvc.perform(
            post(usersUrl)
                .header("X-User-Id", testAdminUserId.toString())
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        )
    }

    @Test
    fun `POST auth login should return 200 and JWT token for valid credentials`() {
        val loginRequest = LoginRequest(username = "authuser", password = "Test@123")

        mockMvc
            .perform(
                post(authUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.token").isString)
            .andExpect(jsonPath("$.expiresAt").exists())
            .andExpect(jsonPath("$.username").value("authuser"))
            .andExpect(jsonPath("$.role").value("USER"))
    }

    @Test
    fun `POST auth login should return 401 for invalid username`() {
        val loginRequest = LoginRequest(username = "nonexistent", password = "Test@123")

        mockMvc
            .perform(
                post(authUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Invalid username or password"))
    }

    @Test
    fun `POST auth login should return 401 for invalid password`() {
        val loginRequest = LoginRequest(username = "authuser", password = "WrongPassword")

        mockMvc
            .perform(
                post(authUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Invalid username or password"))
    }

    @Test
    fun `POST auth login should return 400 for empty username`() {
        val loginRequest = LoginRequest(username = "", password = "Test@123")

        mockMvc
            .perform(
                post(authUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST auth login should return 400 for empty password`() {
        val loginRequest = LoginRequest(username = "authuser", password = "")

        mockMvc
            .perform(
                post(authUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)),
            ).andExpect(status().isBadRequest)
    }
}
