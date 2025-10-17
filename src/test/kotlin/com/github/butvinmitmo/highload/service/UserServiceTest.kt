package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.dto.CreateUserRequest
import com.github.butvinmitmo.highload.dto.UpdateUserRequest
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
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserServiceTest {
    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var userService: UserService
    private val userMapper = UserMapper()

    private val userId = UUID.randomUUID()
    private val createdAt = Instant.now()

    @BeforeEach
    fun setup() {
        userService = UserService(userRepository, userMapper)
    }

    private fun createUser(
        id: UUID,
        username: String,
        createdAt: Instant = this.createdAt,
    ): User {
        val user = User(username = username)

        val idField = User::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)

        val createdAtField = User::class.java.getDeclaredField("createdAt")
        createdAtField.isAccessible = true
        createdAtField.set(user, createdAt)

        return user
    }

    @Test
    fun `createUser should create new user successfully`() {
        // Given
        val request = CreateUserRequest(username = "testuser")
        val savedUser = createUser(id = userId, username = "testuser")

        whenever(userRepository.findByUsername("testuser")).thenReturn(null)
        whenever(userRepository.save(any())).thenReturn(savedUser)

        // When
        val result = userService.createUser(request)

        // Then
        assertNotNull(result)
        assertEquals(userId, result.id)

        // Verify the user was saved with correct data
        val userCaptor = argumentCaptor<User>()
        verify(userRepository).save(userCaptor.capture())
        assertEquals("testuser", userCaptor.firstValue.username)
    }

    @Test
    fun `createUser should throw ConflictException when user already exists`() {
        // Given
        val request = CreateUserRequest(username = "testuser")
        val existingUser = createUser(id = userId, username = "testuser")

        whenever(userRepository.findByUsername("testuser")).thenReturn(existingUser)

        // When/Then
        val exception =
            assertThrows<ConflictException> {
                userService.createUser(request)
            }
        assertEquals("User with this username already exists", exception.message)

        // Verify save was never called
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `getUser should return user when found`() {
        // Given
        val user = createUser(id = userId, username = "testuser")

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

        // When
        val result = userService.getUser(userId)

        // Then
        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals("testuser", result.username)
    }

    @Test
    fun `getUser should throw NotFoundException when user not found`() {
        // Given
        whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                userService.getUser(userId)
            }
        assertEquals("User not found", exception.message)
    }

    @Test
    fun `updateUser should update username when provided`() {
        // Given
        val existingUser = createUser(id = userId, username = "oldname")
        val updateRequest = UpdateUserRequest(username = "newname")

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(existingUser))
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as User }

        // When
        val result = userService.updateUser(userId, updateRequest)

        // Then
        assertNotNull(result)
        assertEquals("newname", result.username)

        // Verify the user was saved with updated username
        val userCaptor = argumentCaptor<User>()
        verify(userRepository).save(userCaptor.capture())
        assertEquals("newname", userCaptor.firstValue.username)
    }

    @Test
    fun `updateUser should throw NotFoundException when user not found`() {
        // Given
        val updateRequest = UpdateUserRequest(username = "newname")

        whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                userService.updateUser(userId, updateRequest)
            }
        assertEquals("User not found", exception.message)

        // Verify save was never called
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `deleteUser should delete user when exists`() {
        // Given
        whenever(userRepository.existsById(userId)).thenReturn(true)

        // When
        userService.deleteUser(userId)

        // Then
        verify(userRepository).deleteById(userId)
    }

    @Test
    fun `deleteUser should throw NotFoundException when user not found`() {
        // Given
        whenever(userRepository.existsById(userId)).thenReturn(false)

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                userService.deleteUser(userId)
            }
        assertEquals("User not found", exception.message)

        // Verify delete was never called
        verify(userRepository, never()).deleteById(any())
    }

    @Test
    fun `getUsers should return paginated users`() {
        // Given
        val users =
            listOf(
                createUser(id = UUID.randomUUID(), username = "user1"),
                createUser(id = UUID.randomUUID(), username = "user2"),
            )
        val pageable = PageRequest.of(0, 2)
        val page = PageImpl(users, pageable, 2)

        whenever(userRepository.findAll(pageable)).thenReturn(page)

        // When
        val result = userService.getUsers(0, 2)

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("user1", result[0].username)
        assertEquals("user2", result[1].username)
    }

    @Test
    fun `getUsers should return empty list when no users exist`() {
        // Given
        val pageable = PageRequest.of(0, 10)
        val emptyPage = PageImpl<User>(emptyList(), pageable, 0)

        whenever(userRepository.findAll(pageable)).thenReturn(emptyPage)

        // When
        val result = userService.getUsers(0, 10)

        // Then
        assertNotNull(result)
        assertEquals(0, result.size)
    }

    @Test
    fun `updateUser should not change username when null provided`() {
        // Given
        val existingUser = createUser(id = userId, username = "originalname")
        val updateRequest = UpdateUserRequest(username = null)

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(existingUser))
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as User }

        // When
        val result = userService.updateUser(userId, updateRequest)

        // Then
        assertNotNull(result)
        assertEquals("originalname", result.username)

        // Verify the user was saved without username change
        val userCaptor = argumentCaptor<User>()
        verify(userRepository).save(userCaptor.capture())
        assertEquals("originalname", userCaptor.firstValue.username)
    }

    @Test
    fun `updateUser should keep same ID`() {
        // Given
        val existingUser = createUser(id = userId, username = "oldname")
        val updateRequest = UpdateUserRequest(username = "newname")

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(existingUser))
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as User }

        // When
        val result = userService.updateUser(userId, updateRequest)

        // Then
        assertEquals(userId, result.id)
    }

    @Test
    fun `getUserEntity should return user when found`() {
        // Given
        val user = createUser(id = userId, username = "testuser")

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

        // When
        val result = userService.getUserEntity(userId)

        // Then
        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals("testuser", result.username)
    }

    @Test
    fun `getUserEntity should throw NotFoundException when user not found`() {
        // Given
        whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                userService.getUserEntity(userId)
            }
        assertEquals("User not found", exception.message)
    }
}
