package com.github.butvinmitmo.userservice.integration.controller

import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.CreateUserResponse
import com.github.butvinmitmo.shared.dto.UpdateUserRequest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import java.util.UUID

class UserControllerIntegrationTest : BaseControllerIntegrationTest() {
    private val baseUrl = "/api/v0.0.1/users"
    private val testAdminUserId = UUID.fromString("10000000-0000-0000-0000-000000000001")

    @Test
    fun `POST users should create user and return 201`() {
        val request = CreateUserRequest(username = "newuser", password = "Test@123")

        webTestClient
            .post()
            .uri(baseUrl)
            .header("X-User-Id", testAdminUserId.toString())
            .header("X-User-Role", "ADMIN")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody()
            .jsonPath("$.id")
            .exists()
    }

    @Test
    fun `POST users should return 409 for duplicate username`() {
        val request = CreateUserRequest(username = "duplicateuser", password = "Test@123")

        webTestClient
            .post()
            .uri(baseUrl)
            .header("X-User-Id", testAdminUserId.toString())
            .header("X-User-Role", "ADMIN")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated

        webTestClient
            .post()
            .uri(baseUrl)
            .header("X-User-Id", testAdminUserId.toString())
            .header("X-User-Role", "ADMIN")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isEqualTo(409)
    }

    @Test
    fun `GET users should return paginated list with X-Total-Count header`() {
        val request = CreateUserRequest(username = "listuser", password = "Test@123")
        webTestClient
            .post()
            .uri(baseUrl)
            .header("X-User-Id", testAdminUserId.toString())
            .header("X-User-Role", "ADMIN")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated

        webTestClient
            .get()
            .uri(baseUrl)
            .header("X-User-Id", testAdminUserId.toString())
            .header("X-User-Role", "ADMIN")
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .exists("X-Total-Count")
            .expectBody()
            .jsonPath("$")
            .isArray
    }

    @Test
    fun `GET users by id should return user`() {
        val createRequest = CreateUserRequest(username = "getbyiduser", password = "Test@123")
        val createResponse =
            webTestClient
                .post()
                .uri(baseUrl)
                .header("X-User-Id", testAdminUserId.toString())
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody(CreateUserResponse::class.java)
                .returnResult()
                .responseBody!!

        val userId = createResponse.id

        webTestClient
            .get()
            .uri("$baseUrl/$userId")
            .header("X-User-Id", testAdminUserId.toString())
            .header("X-User-Role", "ADMIN")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.id")
            .isEqualTo(userId.toString())
            .jsonPath("$.username")
            .isEqualTo("getbyiduser")
    }

    @Test
    fun `GET users by id should return 404 for non-existent user`() {
        val nonExistentId = UUID.randomUUID()

        webTestClient
            .get()
            .uri("$baseUrl/$nonExistentId")
            .header("X-User-Id", testAdminUserId.toString())
            .header("X-User-Role", "ADMIN")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `PUT users should update user`() {
        val createRequest = CreateUserRequest(username = "originaluser", password = "Test@123")
        val createResponse =
            webTestClient
                .post()
                .uri(baseUrl)
                .header("X-User-Id", testAdminUserId.toString())
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody(CreateUserResponse::class.java)
                .returnResult()
                .responseBody!!

        val userId = createResponse.id

        val updateRequest = UpdateUserRequest(username = "updateduser")
        webTestClient
            .put()
            .uri("$baseUrl/$userId")
            .header("X-User-Id", testAdminUserId.toString())
            .header("X-User-Role", "ADMIN")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.username")
            .isEqualTo("updateduser")
    }

    @Test
    fun `DELETE users should delete user and return 204`() {
        val createRequest = CreateUserRequest(username = "todelete", password = "Test@123")
        val createResponse =
            webTestClient
                .post()
                .uri(baseUrl)
                .header("X-User-Id", testAdminUserId.toString())
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody(CreateUserResponse::class.java)
                .returnResult()
                .responseBody!!

        val userId = createResponse.id

        webTestClient
            .delete()
            .uri("$baseUrl/$userId")
            .header("X-User-Id", testAdminUserId.toString())
            .header("X-User-Role", "ADMIN")
            .exchange()
            .expectStatus()
            .isNoContent

        webTestClient
            .get()
            .uri("$baseUrl/$userId")
            .header("X-User-Id", testAdminUserId.toString())
            .header("X-User-Role", "ADMIN")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `DELETE users should return 404 for non-existent user`() {
        val nonExistentId = UUID.randomUUID()

        webTestClient
            .delete()
            .uri("$baseUrl/$nonExistentId")
            .header("X-User-Id", testAdminUserId.toString())
            .header("X-User-Role", "ADMIN")
            .exchange()
            .expectStatus()
            .isNotFound
    }
}
