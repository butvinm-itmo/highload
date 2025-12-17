package com.github.butvinmitmo.userservice.unit.service

import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.LoginRequest
import com.github.butvinmitmo.shared.dto.UpdateUserRequest
import com.github.butvinmitmo.userservice.TestEntityFactory
import com.github.butvinmitmo.userservice.entity.Role
import com.github.butvinmitmo.userservice.entity.RoleType
import com.github.butvinmitmo.userservice.entity.User
import com.github.butvinmitmo.userservice.exception.ConflictException
import com.github.butvinmitmo.userservice.exception.NotFoundException
import com.github.butvinmitmo.userservice.exception.UnauthorizedException
import com.github.butvinmitmo.userservice.mapper.UserMapper
import com.github.butvinmitmo.userservice.repository.RoleRepository
import com.github.butvinmitmo.userservice.repository.UserRepository
import com.github.butvinmitmo.userservice.security.JwtUtil
import com.github.butvinmitmo.userservice.service.UserService
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
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserServiceTest {
    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var roleRepository: RoleRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var jwtUtil: JwtUtil

    private lateinit var userService: UserService
    private val userMapper = UserMapper()

    private val userId = UUID.randomUUID()
    private val createdAt = Instant.now()
    private val testUserRole = Role(id = RoleType.USER_ID, name = "USER")

    @BeforeEach
    fun setup() {
        userService = UserService(userRepository, roleRepository, userMapper, passwordEncoder, jwtUtil)
    }

    @Test
    fun `createUser should create new user successfully`() {
        val request = CreateUserRequest(username = "testuser", password = "Test@123")
        val savedUser = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = createdAt)

        whenever(userRepository.findByUsername("testuser")).thenReturn(null)
        whenever(roleRepository.findByName("USER")).thenReturn(testUserRole)
        whenever(passwordEncoder.encode("Test@123")).thenReturn("hashedPassword")
        whenever(userRepository.save(any())).thenReturn(savedUser)

        val result = userService.createUser(request)

        assertNotNull(result)
        assertEquals(userId, result.id)

        val userCaptor = argumentCaptor<User>()
        verify(userRepository).save(userCaptor.capture())
        assertEquals("testuser", userCaptor.firstValue.username)
    }

    @Test
    fun `createUser should throw ConflictException when user already exists`() {
        val request = CreateUserRequest(username = "testuser", password = "Test@123")
        val existingUser = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = createdAt)

        whenever(userRepository.findByUsername("testuser")).thenReturn(existingUser)

        val exception =
            assertThrows<ConflictException> {
                userService.createUser(request)
            }
        assertEquals("User with this username already exists", exception.message)

        verify(userRepository, never()).save(any())
    }

    @Test
    fun `getUser should return user when found`() {
        val user = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = createdAt)

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

        val result = userService.getUser(userId)

        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals("testuser", result.username)
    }

    @Test
    fun `getUser should throw NotFoundException when user not found`() {
        whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

        val exception =
            assertThrows<NotFoundException> {
                userService.getUser(userId)
            }
        assertEquals("User not found", exception.message)
    }

    @Test
    fun `updateUser should update username when provided`() {
        val existingUser = TestEntityFactory.createUser(id = userId, username = "oldname", createdAt = createdAt)
        val updateRequest = UpdateUserRequest(username = "newname")

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(existingUser))
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as User }

        val result = userService.updateUser(userId, updateRequest)

        assertNotNull(result)
        assertEquals("newname", result.username)

        val userCaptor = argumentCaptor<User>()
        verify(userRepository).save(userCaptor.capture())
        assertEquals("newname", userCaptor.firstValue.username)
    }

    @Test
    fun `updateUser should throw NotFoundException when user not found`() {
        val updateRequest = UpdateUserRequest(username = "newname")

        whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

        val exception =
            assertThrows<NotFoundException> {
                userService.updateUser(userId, updateRequest)
            }
        assertEquals("User not found", exception.message)

        verify(userRepository, never()).save(any())
    }

    @Test
    fun `deleteUser should delete user when exists`() {
        whenever(userRepository.existsById(userId)).thenReturn(true)

        userService.deleteUser(userId)

        verify(userRepository).deleteById(userId)
    }

    @Test
    fun `deleteUser should throw NotFoundException when user not found`() {
        whenever(userRepository.existsById(userId)).thenReturn(false)

        val exception =
            assertThrows<NotFoundException> {
                userService.deleteUser(userId)
            }
        assertEquals("User not found", exception.message)

        verify(userRepository, never()).deleteById(any())
    }

    @Test
    fun `getUsers should return paginated users`() {
        val users =
            listOf(
                TestEntityFactory.createUser(id = UUID.randomUUID(), username = "user1", createdAt = createdAt),
                TestEntityFactory.createUser(id = UUID.randomUUID(), username = "user2", createdAt = createdAt),
            )
        val pageable = PageRequest.of(0, 2)
        val page = PageImpl(users, pageable, 2)

        whenever(userRepository.findAll(pageable)).thenReturn(page)

        val result = userService.getUsers(0, 2)

        assertNotNull(result)
        assertEquals(2, result.content.size)
        assertEquals("user1", result.content[0].username)
        assertEquals("user2", result.content[1].username)
    }

    @Test
    fun `getUsers should return empty list when no users exist`() {
        val pageable = PageRequest.of(0, 10)
        val emptyPage = PageImpl<User>(emptyList(), pageable, 0)

        whenever(userRepository.findAll(pageable)).thenReturn(emptyPage)

        val result = userService.getUsers(0, 10)

        assertNotNull(result)
        assertEquals(0, result.content.size)
    }

    @Test
    fun `authenticate should return token for valid credentials`() {
        val request = LoginRequest(username = "testuser", password = "Test@123")
        val user = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = createdAt)
        val expiresAt = Instant.now().plusSeconds(86400)

        whenever(userRepository.findByUsername("testuser")).thenReturn(user)
        whenever(passwordEncoder.matches("Test@123", user.passwordHash)).thenReturn(true)
        whenever(jwtUtil.generateToken(user)).thenReturn(Pair("mock-jwt-token", expiresAt))

        val result = userService.authenticate(request)

        assertNotNull(result)
        assertEquals("mock-jwt-token", result.token)
        assertEquals(expiresAt, result.expiresAt)
        assertEquals("testuser", result.username)
        assertEquals("USER", result.role)
        verify(jwtUtil).generateToken(user)
    }

    @Test
    fun `authenticate should throw UnauthorizedException for invalid username`() {
        val request = LoginRequest(username = "nonexistent", password = "Test@123")

        whenever(userRepository.findByUsername("nonexistent")).thenReturn(null)

        val exception =
            assertThrows<UnauthorizedException> {
                userService.authenticate(request)
            }
        assertEquals("Invalid username or password", exception.message)

        verify(passwordEncoder, never()).matches(any(), any())
        verify(jwtUtil, never()).generateToken(any())
    }

    @Test
    fun `authenticate should throw UnauthorizedException for invalid password`() {
        val request = LoginRequest(username = "testuser", password = "WrongPassword")
        val user = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = createdAt)

        whenever(userRepository.findByUsername("testuser")).thenReturn(user)
        whenever(passwordEncoder.matches("WrongPassword", user.passwordHash)).thenReturn(false)

        val exception =
            assertThrows<UnauthorizedException> {
                userService.authenticate(request)
            }
        assertEquals("Invalid username or password", exception.message)

        verify(jwtUtil, never()).generateToken(any())
    }
}
