package com.github.butvinmitmo.userservice.integration.service

import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.UpdateUserRequest
import com.github.butvinmitmo.userservice.entity.RoleType
import com.github.butvinmitmo.userservice.exception.ConflictException
import com.github.butvinmitmo.userservice.exception.NotFoundException
import com.github.butvinmitmo.userservice.integration.BaseIntegrationTest
import com.github.butvinmitmo.userservice.repository.RoleRepository
import com.github.butvinmitmo.userservice.service.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.test.StepVerifier
import java.util.UUID

class UserServiceIntegrationTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Test
    fun `createUser should persist user to database`() {
        val request = CreateUserRequest(username = "integrationuser", password = "Test@123")

        val response = userService.createUser(request).block()!!

        assertNotNull(response.id)
        val savedUser = userRepository.findById(response.id).block()
        assertNotNull(savedUser)
        assertEquals("integrationuser", savedUser!!.username)
    }

    @Test
    fun `createUser should throw ConflictException for duplicate username`() {
        val request = CreateUserRequest(username = "duplicateuser", password = "Test@123")
        userService.createUser(request).block()

        StepVerifier
            .create(userService.createUser(request))
            .expectError(ConflictException::class.java)
            .verify()
    }

    @Test
    fun `getUser should return existing user`() {
        val createResponse =
            userService
                .createUser(
                    CreateUserRequest(username = "getuser", password = "Test@123"),
                ).block()!!

        val user = userService.getUser(createResponse.id).block()!!

        assertEquals(createResponse.id, user.id)
        assertEquals("getuser", user.username)
    }

    @Test
    fun `getUser should throw NotFoundException for non-existent user`() {
        val nonExistentId = UUID.randomUUID()

        StepVerifier
            .create(userService.getUser(nonExistentId))
            .expectError(NotFoundException::class.java)
            .verify()
    }

    @Test
    fun `updateUser should update username`() {
        val createResponse =
            userService
                .createUser(
                    CreateUserRequest(username = "originalname", password = "Test@123"),
                ).block()!!

        val updated = userService.updateUser(createResponse.id, UpdateUserRequest(username = "updatedname")).block()!!

        assertEquals("updatedname", updated.username)
        val savedUser = userRepository.findById(createResponse.id).block()!!
        assertEquals("updatedname", savedUser.username)
    }

    @Test
    fun `deleteUser should remove user from database`() {
        val createResponse =
            userService
                .createUser(
                    CreateUserRequest(username = "todelete", password = "Test@123"),
                ).block()!!

        userService.deleteUser(createResponse.id).block()

        val exists = userRepository.existsById(createResponse.id).block()!!
        assertTrue(!exists)
    }

    @Test
    fun `deleteUser should throw NotFoundException for non-existent user`() {
        val nonExistentId = UUID.randomUUID()

        StepVerifier
            .create(userService.deleteUser(nonExistentId))
            .expectError(NotFoundException::class.java)
            .verify()
    }

    @Test
    fun `getUsers should return paginated list`() {
        userService.createUser(CreateUserRequest(username = "pageuser1", password = "Test@123")).block()
        userService.createUser(CreateUserRequest(username = "pageuser2", password = "Test@123")).block()

        val result = userService.getUsers(0, 10).block()!!

        assertTrue(result.content.size >= 2)
    }

    @Test
    fun `createUser should create user with MEDIUM role when specified`() {
        val request = CreateUserRequest(username = "mediumuser", password = "Test@123", role = "MEDIUM")

        val response = userService.createUser(request).block()!!

        assertNotNull(response.id)
        val savedUser = userRepository.findById(response.id).block()!!
        assertNotNull(savedUser)
        assertEquals("mediumuser", savedUser.username)
        assertEquals(RoleType.MEDIUM_ID, savedUser.roleId)
    }

    @Test
    fun `createUser should create user with ADMIN role when specified`() {
        val request = CreateUserRequest(username = "adminuser", password = "Test@123", role = "ADMIN")

        val response = userService.createUser(request).block()!!

        assertNotNull(response.id)
        val savedUser = userRepository.findById(response.id).block()!!
        assertNotNull(savedUser)
        assertEquals("adminuser", savedUser.username)
        assertEquals(RoleType.ADMIN_ID, savedUser.roleId)
    }

    @Test
    fun `createUser should default to USER role when role not specified`() {
        val request = CreateUserRequest(username = "defaultroleuser", password = "Test@123")

        val response = userService.createUser(request).block()!!

        assertNotNull(response.id)
        val savedUser = userRepository.findById(response.id).block()!!
        assertNotNull(savedUser)
        assertEquals("defaultroleuser", savedUser.username)
        assertEquals(RoleType.USER_ID, savedUser.roleId)
    }

    @Test
    fun `createUser should throw NotFoundException for invalid role`() {
        val request = CreateUserRequest(username = "invalidroleuser", password = "Test@123", role = "INVALID")

        StepVerifier
            .create(userService.createUser(request))
            .expectError(NotFoundException::class.java)
            .verify()
    }

    @Test
    fun `updateUser should update user role`() {
        val createResponse =
            userService
                .createUser(
                    CreateUserRequest(username = "roleupdate", password = "Test@123"),
                ).block()!!

        val updated = userService.updateUser(createResponse.id, UpdateUserRequest(role = "MEDIUM")).block()!!

        assertEquals("MEDIUM", updated.role)
        val savedUser = userRepository.findById(createResponse.id).block()!!
        assertEquals(RoleType.MEDIUM_ID, savedUser.roleId)
    }

    @Test
    fun `updateUser should throw NotFoundException for invalid role`() {
        val createResponse =
            userService
                .createUser(
                    CreateUserRequest(username = "invalidupdate", password = "Test@123"),
                ).block()!!

        StepVerifier
            .create(userService.updateUser(createResponse.id, UpdateUserRequest(role = "INVALID")))
            .expectError(NotFoundException::class.java)
            .verify()
    }
}
