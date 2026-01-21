package com.github.butvinmitmo.userservice.integration.service

import com.github.butvinmitmo.userservice.application.service.UserService
import com.github.butvinmitmo.userservice.domain.model.RoleType
import com.github.butvinmitmo.userservice.exception.ConflictException
import com.github.butvinmitmo.userservice.exception.NotFoundException
import com.github.butvinmitmo.userservice.infrastructure.persistence.repository.SpringDataRoleRepository
import com.github.butvinmitmo.userservice.integration.BaseIntegrationTest
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
    private lateinit var springDataRoleRepository: SpringDataRoleRepository

    @Test
    fun `createUser should persist user to database`() {
        val response = userService.createUser("integrationuser", "Test@123", null).block()!!

        assertNotNull(response)
        val savedUser = springDataUserRepository.findById(response).block()
        assertNotNull(savedUser)
        assertEquals("integrationuser", savedUser!!.username)
    }

    @Test
    fun `createUser should throw ConflictException for duplicate username`() {
        userService.createUser("duplicateuser", "Test@123", null).block()

        StepVerifier
            .create(userService.createUser("duplicateuser", "Test@123", null))
            .expectError(ConflictException::class.java)
            .verify()
    }

    @Test
    fun `getUser should return existing user`() {
        val createResponse = userService.createUser("getuser", "Test@123", null).block()!!

        val user = userService.getUser(createResponse).block()!!

        assertEquals(createResponse, user.id)
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
        val createResponse = userService.createUser("originalname", "Test@123", null).block()!!

        val updated = userService.updateUser(createResponse, "updatedname", null, null).block()!!

        assertEquals("updatedname", updated.username)
        val savedUser = springDataUserRepository.findById(createResponse).block()!!
        assertEquals("updatedname", savedUser.username)
    }

    @Test
    fun `deleteUser should remove user from database`() {
        val createResponse = userService.createUser("todelete", "Test@123", null).block()!!

        userService.deleteUser(createResponse).block()

        val exists = springDataUserRepository.existsById(createResponse).block()!!
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
        userService.createUser("pageuser1", "Test@123", null).block()
        userService.createUser("pageuser2", "Test@123", null).block()

        val result = userService.getUsers(0, 10).block()!!

        assertTrue(result.content.size >= 2)
    }

    @Test
    fun `createUser should create user with MEDIUM role when specified`() {
        val response = userService.createUser("mediumuser", "Test@123", "MEDIUM").block()!!

        assertNotNull(response)
        val savedUser = springDataUserRepository.findById(response).block()!!
        assertNotNull(savedUser)
        assertEquals("mediumuser", savedUser.username)
        assertEquals(RoleType.MEDIUM_ID, savedUser.roleId)
    }

    @Test
    fun `createUser should create user with ADMIN role when specified`() {
        val response = userService.createUser("adminuser", "Test@123", "ADMIN").block()!!

        assertNotNull(response)
        val savedUser = springDataUserRepository.findById(response).block()!!
        assertNotNull(savedUser)
        assertEquals("adminuser", savedUser.username)
        assertEquals(RoleType.ADMIN_ID, savedUser.roleId)
    }

    @Test
    fun `createUser should default to USER role when role not specified`() {
        val response = userService.createUser("defaultroleuser", "Test@123", null).block()!!

        assertNotNull(response)
        val savedUser = springDataUserRepository.findById(response).block()!!
        assertNotNull(savedUser)
        assertEquals("defaultroleuser", savedUser.username)
        assertEquals(RoleType.USER_ID, savedUser.roleId)
    }

    @Test
    fun `createUser should throw NotFoundException for invalid role`() {
        StepVerifier
            .create(userService.createUser("invalidroleuser", "Test@123", "INVALID"))
            .expectError(NotFoundException::class.java)
            .verify()
    }

    @Test
    fun `updateUser should update user role`() {
        val createResponse = userService.createUser("roleupdate", "Test@123", null).block()!!

        val updated = userService.updateUser(createResponse, null, null, "MEDIUM").block()!!

        assertEquals("MEDIUM", updated.role.name)
        val savedUser = springDataUserRepository.findById(createResponse).block()!!
        assertEquals(RoleType.MEDIUM_ID, savedUser.roleId)
    }

    @Test
    fun `updateUser should throw NotFoundException for invalid role`() {
        val createResponse = userService.createUser("invalidupdate", "Test@123", null).block()!!

        StepVerifier
            .create(userService.updateUser(createResponse, null, null, "INVALID"))
            .expectError(NotFoundException::class.java)
            .verify()
    }
}
