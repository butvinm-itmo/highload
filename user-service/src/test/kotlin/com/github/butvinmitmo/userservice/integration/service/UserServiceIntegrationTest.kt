package com.github.butvinmitmo.userservice.integration.service

import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.UpdateUserRequest
import com.github.butvinmitmo.userservice.exception.ConflictException
import com.github.butvinmitmo.userservice.exception.NotFoundException
import com.github.butvinmitmo.userservice.integration.BaseIntegrationTest
import com.github.butvinmitmo.userservice.service.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class UserServiceIntegrationTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var userService: UserService

    @Test
    fun `createUser should persist user to database`() {
        val request = CreateUserRequest(username = "integrationuser", password = "Test@123")

        val response = userService.createUser(request)

        assertNotNull(response.id)
        val savedUser = userRepository.findById(response.id).orElse(null)
        assertNotNull(savedUser)
        assertEquals("integrationuser", savedUser.username)
    }

    @Test
    fun `createUser should throw ConflictException for duplicate username`() {
        val request = CreateUserRequest(username = "duplicateuser", password = "Test@123")
        userService.createUser(request)

        assertThrows<ConflictException> {
            userService.createUser(request)
        }
    }

    @Test
    fun `getUser should return existing user`() {
        val createResponse = userService.createUser(CreateUserRequest(username = "getuser", password = "Test@123"))

        val user = userService.getUser(createResponse.id)

        assertEquals(createResponse.id, user.id)
        assertEquals("getuser", user.username)
    }

    @Test
    fun `getUser should throw NotFoundException for non-existent user`() {
        val nonExistentId = UUID.randomUUID()

        assertThrows<NotFoundException> {
            userService.getUser(nonExistentId)
        }
    }

    @Test
    fun `updateUser should update username`() {
        val createResponse = userService.createUser(CreateUserRequest(username = "originalname", password = "Test@123"))

        val updated = userService.updateUser(createResponse.id, UpdateUserRequest(username = "updatedname"))

        assertEquals("updatedname", updated.username)
        val savedUser = userRepository.findById(createResponse.id).orElse(null)
        assertEquals("updatedname", savedUser.username)
    }

    @Test
    fun `deleteUser should remove user from database`() {
        val createResponse = userService.createUser(CreateUserRequest(username = "todelete", password = "Test@123"))

        userService.deleteUser(createResponse.id)

        assertTrue(userRepository.findById(createResponse.id).isEmpty)
    }

    @Test
    fun `deleteUser should throw NotFoundException for non-existent user`() {
        val nonExistentId = UUID.randomUUID()

        assertThrows<NotFoundException> {
            userService.deleteUser(nonExistentId)
        }
    }

    @Test
    fun `getUsers should return paginated list`() {
        userService.createUser(CreateUserRequest(username = "pageuser1", password = "Test@123"))
        userService.createUser(CreateUserRequest(username = "pageuser2", password = "Test@123"))

        val result = userService.getUsers(0, 10)

        assertTrue(result.content.size >= 2)
    }
}
