package com.github.butvinmitmo.userservice.unit.service

import com.github.butvinmitmo.shared.client.DivinationServiceInternalClient
import com.github.butvinmitmo.shared.client.ServiceUnavailableException
import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.LoginRequest
import com.github.butvinmitmo.shared.dto.UpdateUserRequest
import com.github.butvinmitmo.userservice.TestEntityFactory
import com.github.butvinmitmo.userservice.entity.RoleType
import com.github.butvinmitmo.userservice.entity.User
import com.github.butvinmitmo.userservice.exception.ConflictException
import com.github.butvinmitmo.userservice.exception.NotFoundException
import com.github.butvinmitmo.userservice.exception.UnauthorizedException
import com.github.butvinmitmo.userservice.mapper.UserMapper
import com.github.butvinmitmo.userservice.repository.UserRepository
import com.github.butvinmitmo.userservice.security.JwtUtil
import com.github.butvinmitmo.userservice.service.RoleService
import com.github.butvinmitmo.userservice.service.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserServiceTest {
    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var roleService: RoleService

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var jwtUtil: JwtUtil

    @Mock
    private lateinit var divinationServiceInternalClient: DivinationServiceInternalClient

    private lateinit var userService: UserService
    private val userMapper = UserMapper()

    private val userId = UUID.randomUUID()
    private val createdAt = Instant.now()
    private val testUserRole = TestEntityFactory.testUserRole

    @BeforeEach
    fun setup() {
        userService =
            UserService(
                userRepository,
                roleService,
                userMapper,
                passwordEncoder,
                jwtUtil,
                divinationServiceInternalClient,
            )
    }

    @Test
    fun `createUser should create new user successfully`() {
        val request = CreateUserRequest(username = "testuser", password = "Test@123")
        val savedUser = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = createdAt)

        whenever(userRepository.findByUsername("testuser")).thenReturn(Mono.empty())
        whenever(roleService.getRoleByName(null)).thenReturn(Mono.just(testUserRole))
        whenever(passwordEncoder.encode("Test@123")).thenReturn("hashedPassword")
        whenever(userRepository.save(any<User>())).thenReturn(Mono.just(savedUser))

        StepVerifier
            .create(userService.createUser(request))
            .assertNext { result ->
                assertNotNull(result)
                assertEquals(userId, result.id)
            }.verifyComplete()

        val userCaptor = argumentCaptor<User>()
        verify(userRepository).save(userCaptor.capture())
        assertEquals("testuser", userCaptor.firstValue.username)
    }

    @Test
    fun `createUser should throw ConflictException when user already exists`() {
        val request = CreateUserRequest(username = "testuser", password = "Test@123")
        val existingUser = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = createdAt)

        whenever(userRepository.findByUsername("testuser")).thenReturn(Mono.just(existingUser))

        StepVerifier
            .create(userService.createUser(request))
            .expectErrorMatches { it is ConflictException && it.message == "User with this username already exists" }
            .verify()

        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `getUser should return user when found`() {
        val user = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = createdAt)

        whenever(userRepository.findById(userId)).thenReturn(Mono.just(user))
        whenever(roleService.getRoleById(RoleType.USER_ID)).thenReturn(Mono.just(testUserRole))

        StepVerifier
            .create(userService.getUser(userId))
            .assertNext { result ->
                assertNotNull(result)
                assertEquals(userId, result.id)
                assertEquals("testuser", result.username)
            }.verifyComplete()
    }

    @Test
    fun `getUser should throw NotFoundException when user not found`() {
        whenever(userRepository.findById(userId)).thenReturn(Mono.empty())

        StepVerifier
            .create(userService.getUser(userId))
            .expectErrorMatches { it is NotFoundException && it.message == "User not found" }
            .verify()
    }

    @Test
    fun `updateUser should update username when provided`() {
        val existingUser = TestEntityFactory.createUser(id = userId, username = "oldname", createdAt = createdAt)
        val updateRequest = UpdateUserRequest(username = "newname")

        whenever(userRepository.findById(userId)).thenReturn(Mono.just(existingUser))
        whenever(userRepository.save(any<User>())).thenAnswer { Mono.just(it.arguments[0] as User) }
        whenever(roleService.getRoleById(RoleType.USER_ID)).thenReturn(Mono.just(testUserRole))

        StepVerifier
            .create(userService.updateUser(userId, updateRequest))
            .assertNext { result ->
                assertNotNull(result)
                assertEquals("newname", result.username)
            }.verifyComplete()

        val userCaptor = argumentCaptor<User>()
        verify(userRepository).save(userCaptor.capture())
        assertEquals("newname", userCaptor.firstValue.username)
    }

    @Test
    fun `updateUser should throw NotFoundException when user not found`() {
        val updateRequest = UpdateUserRequest(username = "newname")

        whenever(userRepository.findById(userId)).thenReturn(Mono.empty())

        StepVerifier
            .create(userService.updateUser(userId, updateRequest))
            .expectErrorMatches { it is NotFoundException && it.message == "User not found" }
            .verify()

        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `deleteUser should delete user when exists and cleanup succeeds`() {
        whenever(userRepository.existsById(userId)).thenReturn(Mono.just(true))
        whenever(divinationServiceInternalClient.deleteUserData(userId)).thenReturn(ResponseEntity.noContent().build())
        whenever(userRepository.deleteById(userId)).thenReturn(Mono.empty())

        StepVerifier
            .create(userService.deleteUser(userId))
            .verifyComplete()

        verify(divinationServiceInternalClient).deleteUserData(userId)
        verify(userRepository).deleteById(userId)
    }

    @Test
    fun `deleteUser should throw NotFoundException when user not found`() {
        whenever(userRepository.existsById(userId)).thenReturn(Mono.just(false))

        StepVerifier
            .create(userService.deleteUser(userId))
            .expectErrorMatches { it is NotFoundException && it.message == "User not found" }
            .verify()

        verify(divinationServiceInternalClient, never()).deleteUserData(any<UUID>())
        verify(userRepository, never()).deleteById(any<UUID>())
    }

    @Test
    fun `deleteUser should throw ServiceUnavailableException when cleanup fails`() {
        whenever(userRepository.existsById(userId)).thenReturn(Mono.just(true))
        whenever(divinationServiceInternalClient.deleteUserData(userId))
            .thenThrow(ServiceUnavailableException("divination-service"))

        StepVerifier
            .create(userService.deleteUser(userId))
            .expectErrorMatches { it is ServiceUnavailableException && it.serviceName == "divination-service" }
            .verify()

        verify(divinationServiceInternalClient).deleteUserData(userId)
        verify(userRepository, never()).deleteById(any<UUID>())
    }

    @Test
    fun `getUsers should return paginated users`() {
        val users =
            listOf(
                TestEntityFactory.createUser(id = UUID.randomUUID(), username = "user1", createdAt = createdAt),
                TestEntityFactory.createUser(id = UUID.randomUUID(), username = "user2", createdAt = createdAt),
            )

        whenever(userRepository.count()).thenReturn(Mono.just(2L))
        whenever(userRepository.findAllPaginated(0L, 2)).thenReturn(Flux.fromIterable(users))
        whenever(roleService.getRoleById(RoleType.USER_ID)).thenReturn(Mono.just(testUserRole))

        StepVerifier
            .create(userService.getUsers(0, 2))
            .assertNext { result ->
                assertNotNull(result)
                assertEquals(2, result.content.size)
                assertEquals("user1", result.content[0].username)
                assertEquals("user2", result.content[1].username)
            }.verifyComplete()
    }

    @Test
    fun `getUsers should return empty list when no users exist`() {
        whenever(userRepository.count()).thenReturn(Mono.just(0L))
        whenever(userRepository.findAllPaginated(0L, 10)).thenReturn(Flux.empty())

        StepVerifier
            .create(userService.getUsers(0, 10))
            .assertNext { result ->
                assertNotNull(result)
                assertEquals(0, result.content.size)
            }.verifyComplete()
    }

    @Test
    fun `authenticate should return token for valid credentials`() {
        val request = LoginRequest(username = "testuser", password = "Test@123")
        val user = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = createdAt)
        val expiresAt = Instant.now().plusSeconds(86400)

        whenever(userRepository.findByUsername("testuser")).thenReturn(Mono.just(user))
        whenever(passwordEncoder.matches("Test@123", user.passwordHash)).thenReturn(true)
        whenever(roleService.getRoleById(RoleType.USER_ID)).thenReturn(Mono.just(testUserRole))
        whenever(jwtUtil.generateToken(user, testUserRole)).thenReturn(Pair("mock-jwt-token", expiresAt))

        StepVerifier
            .create(userService.authenticate(request))
            .assertNext { result ->
                assertNotNull(result)
                assertEquals("mock-jwt-token", result.token)
                assertEquals(expiresAt, result.expiresAt)
                assertEquals("testuser", result.username)
                assertEquals("USER", result.role)
            }.verifyComplete()

        verify(jwtUtil).generateToken(user, testUserRole)
    }

    @Test
    fun `authenticate should throw UnauthorizedException for invalid username`() {
        val request = LoginRequest(username = "nonexistent", password = "Test@123")

        whenever(userRepository.findByUsername("nonexistent")).thenReturn(Mono.empty())

        StepVerifier
            .create(userService.authenticate(request))
            .expectErrorMatches { it is UnauthorizedException && it.message == "Invalid username or password" }
            .verify()

        verify(passwordEncoder, never()).matches(any(), any())
        verify(jwtUtil, never()).generateToken(any(), any())
    }

    @Test
    fun `authenticate should throw UnauthorizedException for invalid password`() {
        val request = LoginRequest(username = "testuser", password = "WrongPassword")
        val user = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = createdAt)

        whenever(userRepository.findByUsername("testuser")).thenReturn(Mono.just(user))
        whenever(passwordEncoder.matches("WrongPassword", user.passwordHash)).thenReturn(false)

        StepVerifier
            .create(userService.authenticate(request))
            .expectErrorMatches { it is UnauthorizedException && it.message == "Invalid username or password" }
            .verify()

        verify(jwtUtil, never()).generateToken(any(), any())
    }
}
