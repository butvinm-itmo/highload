package com.github.butvinmitmo.e2e

import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.UpdateUserRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.util.UUID

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UserServiceE2ETest : BaseE2ETest() {
    companion object {
        private lateinit var testUserId: UUID
        private val testUsername: String = "e2e_user_test_${System.currentTimeMillis()}"
        private val updatedUsername: String = "${testUsername}_updated"
    }

    @Test
    @Order(1)
    fun `GET users should return list with admin user`() {
        val response = userClient.getUsers()

        assertEquals(200, response.statusCode.value())
        val users = response.body!!
        assertTrue(users.isNotEmpty(), "Users list should not be empty")

        val adminUser = users.find { it.username == "admin" }
        assertNotNull(adminUser, "Admin user should exist in database")
    }

    @Test
    @Order(2)
    fun `POST users should create new user`() {
        val request = CreateUserRequest(username = testUsername)
        val response = userClient.createUser(request)

        assertEquals(201, response.statusCode.value())
        assertNotNull(response.body?.id, "Response should contain user ID")
        testUserId = response.body!!.id
    }

    @Test
    @Order(3)
    fun `GET users by id should return user`() {
        val response = userClient.getUserById(testUserId)

        assertEquals(200, response.statusCode.value())
        assertEquals(testUsername, response.body?.username)
        assertEquals(testUserId, response.body?.id)
    }

    @Test
    @Order(4)
    fun `PUT users should update user`() {
        val request = UpdateUserRequest(username = updatedUsername)
        val response = userClient.updateUser(testUserId, request)

        assertEquals(200, response.statusCode.value())
        assertEquals(updatedUsername, response.body?.username)
    }

    @Test
    @Order(5)
    fun `POST users with duplicate username should return 409`() {
        val request = CreateUserRequest(username = updatedUsername)
        assertThrowsWithStatus(409) { userClient.createUser(request) }
    }

    @Test
    @Order(6)
    fun `GET users with non-existent id should return 404`() {
        val fakeId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        assertThrowsWithStatus(404) { userClient.getUserById(fakeId) }
    }

    @Test
    @Order(100)
    fun `DELETE user should succeed and cleanup`() {
        val response = userClient.deleteUser(testUserId)
        assertEquals(204, response.statusCode.value())

        // Verify user is deleted
        assertThrowsWithStatus(404) { userClient.getUserById(testUserId) }
    }
}
