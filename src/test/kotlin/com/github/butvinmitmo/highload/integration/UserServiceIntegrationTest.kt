package com.github.butvinmitmo.highload.integration

import com.github.butvinmitmo.highload.dto.CreateUserRequest
import com.github.butvinmitmo.highload.dto.UpdateUserRequest
import com.github.butvinmitmo.highload.exception.ConflictException
import com.github.butvinmitmo.highload.exception.NotFoundException
import com.github.butvinmitmo.highload.repository.UserRepository
import com.github.butvinmitmo.highload.service.UserService
import org.junit.jupiter.api.AfterEach
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

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `should create user successfully`() {
        val request = CreateUserRequest(username = "testuser")

        val result = userService.createUser(request)

        assertNotNull(result)
        assertNotNull(result.id)

        val saved = userRepository.findById(result.id)
        assertTrue(saved.isPresent)
        assertEquals("testuser", saved.get().username)
        assertNotNull(saved.get().createdAt)
    }

    @Test
    fun `should throw ConflictException when creating user with existing username`() {
        val request = CreateUserRequest(username = "testuser")

        userService.createUser(request)

        val duplicateRequest = CreateUserRequest(username = "testuser")

        val exception =
            assertThrows<ConflictException> {
                userService.createUser(duplicateRequest)
            }
        assertEquals("User with this username already exists", exception.message)
    }

    @Test
    fun `should get user by id successfully`() {
        val request = CreateUserRequest(username = "testuser")

        val created = userService.createUser(request)

        val result = userService.getUser(created.id)

        assertNotNull(result)
        assertEquals(created.id, result.id)
        assertEquals("testuser", result.username)
    }

    @Test
    fun `should throw NotFoundException when getting non-existent user`() {
        val nonExistentId = UUID.randomUUID()

        val exception =
            assertThrows<NotFoundException> {
                userService.getUser(nonExistentId)
            }
        assertEquals("User not found", exception.message)
    }

    @Test
    fun `should update user username successfully`() {
        val createRequest = CreateUserRequest(username = "oldname")

        val created = userService.createUser(createRequest)

        val updateRequest = UpdateUserRequest(username = "newname")
        val result = userService.updateUser(created.id, updateRequest)

        assertNotNull(result)
        assertEquals(created.id, result.id)
        assertEquals("newname", result.username)

        val updated = userRepository.findById(created.id)
        assertTrue(updated.isPresent)
        assertEquals("newname", updated.get().username)
    }

    @Test
    fun `should throw NotFoundException when updating non-existent user`() {
        val nonExistentId = UUID.randomUUID()
        val updateRequest = UpdateUserRequest(username = "newname")

        val exception =
            assertThrows<NotFoundException> {
                userService.updateUser(nonExistentId, updateRequest)
            }
        assertEquals("User not found", exception.message)
    }

    @Test
    fun `should delete user successfully`() {
        val request = CreateUserRequest(username = "testuser")

        val created = userService.createUser(request)

        userService.deleteUser(created.id)

        val deleted = userRepository.findById(created.id)
        assertTrue(deleted.isEmpty)
    }

    @Test
    fun `should throw NotFoundException when deleting non-existent user`() {
        val nonExistentId = UUID.randomUUID()

        val exception =
            assertThrows<NotFoundException> {
                userService.deleteUser(nonExistentId)
            }
        assertEquals("User not found", exception.message)
    }

    @Test
    fun `should get paginated users successfully`() {
        userService.createUser(CreateUserRequest(username = "user1"))
        userService.createUser(CreateUserRequest(username = "user2"))
        userService.createUser(CreateUserRequest(username = "user3"))

        val page0 = userService.getUsers(0, 2)
        assertEquals(2, page0.size)

        val page1 = userService.getUsers(1, 2)
        assertEquals(2, page1.size)

        val allUsers = userService.getUsers(0, 10)
        assertEquals(4, allUsers.size)
    }

    @Test
    fun `should return list with initial admin user`() {
        val result = userService.getUsers(0, 10)

        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("admin", result[0].username)
    }

    @Test
    fun `should get user entity successfully`() {
        val request = CreateUserRequest(username = "testuser")

        val created = userService.createUser(request)

        val result = userService.getUserEntity(created.id)

        assertNotNull(result)
        assertEquals(created.id, result.id)
        assertEquals("testuser", result.username)
    }

    @Test
    fun `should throw NotFoundException when getting non-existent user entity`() {
        val nonExistentId = UUID.randomUUID()

        val exception =
            assertThrows<NotFoundException> {
                userService.getUserEntity(nonExistentId)
            }
        assertEquals("User not found", exception.message)
    }
}
