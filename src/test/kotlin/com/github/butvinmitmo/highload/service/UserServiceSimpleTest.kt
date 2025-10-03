package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.dto.CreateUserRequest
import com.github.butvinmitmo.highload.dto.UpdateUserRequest
import com.github.butvinmitmo.highload.dto.UserDto
import com.github.butvinmitmo.highload.entity.User
import com.github.butvinmitmo.highload.exception.ConflictException
import com.github.butvinmitmo.highload.exception.NotFoundException
import com.github.butvinmitmo.highload.mapper.UserMapper
import com.github.butvinmitmo.highload.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

class UserServiceSimpleTest {

    private lateinit var userService: UserService

    private val userId = UUID.randomUUID()
    private val createdAt = Instant.now()

    // Mock implementations
    private val userRepository = object : UserRepository {
        private val users = mutableMapOf<UUID, User>()

        override fun findById(id: UUID): User? = users[id]

        override fun existsById(id: UUID): Boolean = users.containsKey(id)

        override fun save(user: User): User {
            val saved = if (user.id == null) {
                user.copy(id = UUID.randomUUID())
            } else {
                user
            }
            users[saved.id!!] = saved
            return saved
        }

        override fun findAll(pageable: org.springframework.data.domain.Pageable):
            org.springframework.data.domain.Page<User> {
            val list = users.values.toList()
            val start = minOf(pageable.offset.toInt(), list.size)
            val end = minOf(start + pageable.pageSize, list.size)
            val pageContent = list.subList(start, end)

            return org.springframework.data.support.PageableExecutionUtils.getPage(
                pageContent,
                pageable
            ) { list.size.toLong() }
        }

        override fun deleteById(id: UUID) {
            users.remove(id)
        }
    }

    private val userMapper = UserMapper

    @BeforeEach
    fun setup() {
        userService = UserService(userRepository, userMapper)
    }

    @Test
    fun `createUser should create new user successfully`() {
        // Given
        val request = CreateUserRequest(
            id = userId,
            username = "testuser"
        )

        // When
        val result = userService.createUser(request)

        // Then
        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals("testuser", result.username)
    }

    @Test
    fun `createUser should use default username when not provided`() {
        // Given
        val request = CreateUserRequest(
            id = userId,
            username = null
        )

        // When
        val result = userService.createUser(request)

        // Then
        assertNotNull(result)
        assertEquals("user_$userId", result.username)
    }

    @Test
    fun `createUser should throw ConflictException when user already exists`() {
        // Given
        val request = CreateUserRequest(
            id = userId,
            username = "testuser"
        )

        // Create user first time
        userService.createUser(request)

        // When/Then
        val exception = assertThrows<ConflictException> {
            userService.createUser(request)
        }
        assertEquals("User with this ID already exists", exception.message)
    }

    @Test
    fun `getUser should return user when found`() {
        // Given - create a user first
        val request = CreateUserRequest(
            id = userId,
            username = "testuser"
        )
        userService.createUser(request)

        // When
        val result = userService.getUser(userId)

        // Then
        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals("testuser", result.username)
    }

    @Test
    fun `getUser should throw NotFoundException when user not found`() {
        // When/Then
        val exception = assertThrows<NotFoundException> {
            userService.getUser(userId)
        }
        assertEquals("User not found", exception.message)
    }

    @Test
    fun `updateUser should update username when provided`() {
        // Given - create user first
        val createRequest = CreateUserRequest(
            id = userId,
            username = "testuser"
        )
        userService.createUser(createRequest)

        val updateRequest = UpdateUserRequest(username = "newusername")

        // When
        val result = userService.updateUser(userId, updateRequest)

        // Then
        assertNotNull(result)
        assertEquals("newusername", result.username)
    }

    @Test
    fun `deleteUser should delete user when exists`() {
        // Given - create user first
        val request = CreateUserRequest(
            id = userId,
            username = "testuser"
        )
        userService.createUser(request)

        // When
        userService.deleteUser(userId)

        // Then - should throw not found when trying to get
        assertThrows<NotFoundException> {
            userService.getUser(userId)
        }
    }

    @Test
    fun `deleteUser should throw NotFoundException when user not found`() {
        // When/Then
        val exception = assertThrows<NotFoundException> {
            userService.deleteUser(userId)
        }
        assertEquals("User not found", exception.message)
    }

    @Test
    fun `getUsers should return paginated users`() {
        // Given - create multiple users
        val user1 = userService.createUser(
            CreateUserRequest(id = UUID.randomUUID(), username = "user1")
        )
        val user2 = userService.createUser(
            CreateUserRequest(id = UUID.randomUUID(), username = "user2")
        )
        val user3 = userService.createUser(
            CreateUserRequest(id = UUID.randomUUID(), username = "user3")
        )

        // When
        val result = userService.getUsers(0, 2)

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
    }

    @Test
    fun `getUsers should return empty list when no users exist`() {
        // When
        val result = userService.getUsers(0, 10)

        // Then
        assertNotNull(result)
        assertEquals(0, result.size)
    }

    @Test
    fun `updateUser should throw NotFoundException when user not found`() {
        // Given
        val updateRequest = UpdateUserRequest(username = "newname")

        // When/Then
        val exception = assertThrows<NotFoundException> {
            userService.updateUser(userId, updateRequest)
        }
        assertEquals("User not found", exception.message)
    }

    @Test
    fun `updateUser should keep current username when update request has null username`() {
        // Given - create user first
        val createRequest = CreateUserRequest(
            id = userId,
            username = "originalname"
        )
        userService.createUser(createRequest)

        val updateRequest = UpdateUserRequest(username = null)

        // When
        val result = userService.updateUser(userId, updateRequest)

        // Then
        assertNotNull(result)
        assertEquals("originalname", result.username)
    }

}