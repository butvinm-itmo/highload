package com.github.butvinmitmo.userservice.integration.controller

import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.LoginRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import java.util.UUID

class AuthControllerIntegrationTest : BaseControllerIntegrationTest() {
    private val authUrl = "/api/v0.0.1/auth/login"
    private val usersUrl = "/api/v0.0.1/users"
    private val testAdminUserId = UUID.fromString("10000000-0000-0000-0000-000000000001")

    @BeforeEach
    fun setup() {
        val createRequest = CreateUserRequest(username = "authuser", password = "Test@123")
        webTestClient
            .post()
            .uri(usersUrl)
            .header("X-User-Id", testAdminUserId.toString())
            .header("X-User-Role", "ADMIN")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus()
            .isCreated
    }

    @Test
    fun `POST auth login should return 200 and JWT token for valid credentials`() {
        val loginRequest = LoginRequest(username = "authuser", password = "Test@123")

        webTestClient
            .post()
            .uri(authUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.token")
            .exists()
            .jsonPath("$.token")
            .isNotEmpty
            .jsonPath("$.expiresAt")
            .exists()
            .jsonPath("$.username")
            .isEqualTo("authuser")
            .jsonPath("$.role")
            .isEqualTo("USER")
    }

    @Test
    fun `POST auth login should return 401 for invalid username`() {
        val loginRequest = LoginRequest(username = "nonexistent", password = "Test@123")

        webTestClient
            .post()
            .uri(authUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus()
            .isUnauthorized
            .expectBody()
            .jsonPath("$.message")
            .isEqualTo("Invalid username or password")
    }

    @Test
    fun `POST auth login should return 401 for invalid password`() {
        val loginRequest = LoginRequest(username = "authuser", password = "WrongPassword")

        webTestClient
            .post()
            .uri(authUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus()
            .isUnauthorized
            .expectBody()
            .jsonPath("$.message")
            .isEqualTo("Invalid username or password")
    }

    @Test
    fun `POST auth login should return 400 for empty username`() {
        val loginRequest = LoginRequest(username = "", password = "Test@123")

        webTestClient
            .post()
            .uri(authUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `POST auth login should return 400 for empty password`() {
        val loginRequest = LoginRequest(username = "authuser", password = "")

        webTestClient
            .post()
            .uri(authUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus()
            .isBadRequest
    }
}
