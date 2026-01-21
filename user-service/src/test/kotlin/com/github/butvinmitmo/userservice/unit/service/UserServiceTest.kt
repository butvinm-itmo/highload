package com.github.butvinmitmo.userservice.unit.service

import com.github.butvinmitmo.userservice.TestEntityFactory
import com.github.butvinmitmo.userservice.application.interfaces.provider.PasswordEncoder
import com.github.butvinmitmo.userservice.application.interfaces.provider.TokenProvider
import com.github.butvinmitmo.userservice.application.interfaces.provider.TokenResult
import com.github.butvinmitmo.userservice.application.interfaces.publisher.UserEventPublisher
import com.github.butvinmitmo.userservice.application.interfaces.repository.RoleRepository
import com.github.butvinmitmo.userservice.application.interfaces.repository.UserRepository
import com.github.butvinmitmo.userservice.application.service.UserService
import com.github.butvinmitmo.userservice.domain.model.User
import com.github.butvinmitmo.userservice.exception.ConflictException
import com.github.butvinmitmo.userservice.exception.NotFoundException
import com.github.butvinmitmo.userservice.exception.UnauthorizedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
    private lateinit var roleRepository: RoleRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var tokenProvider: TokenProvider

    @Mock
    private lateinit var userEventPublisher: UserEventPublisher

    private lateinit var userService: UserService

    private val userId = UUID.randomUUID()
    private val createdAt = Instant.now()
    private val testUserRole = TestEntityFactory.testUserRole

    @BeforeEach
    fun setup() {
        lenient().`when`(userEventPublisher.publishCreated(any())).thenReturn(Mono.empty())
        lenient().`when`(userEventPublisher.publishUpdated(any())).thenReturn(Mono.empty())
        lenient().`when`(userEventPublisher.publishDeleted(any())).thenReturn(Mono.empty())

        userService =
            UserService(
                userRepository,
                roleRepository,
                passwordEncoder,
                tokenProvider,
                userEventPublisher,
            )
    }

    @Test
    fun `createUser should create new user successfully`() {
        val savedUser = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = createdAt)

        whenever(userRepository.findByUsername("testuser")).thenReturn(Mono.empty())
        whenever(roleRepository.findByName("USER")).thenReturn(Mono.just(testUserRole))
        whenever(passwordEncoder.encode("Test@123")).thenReturn("hashedPassword")
        whenever(userRepository.save(any<User>())).thenReturn(Mono.just(savedUser))

        StepVerifier
            .create(userService.createUser("testuser", "Test@123", null))
            .assertNext { result ->
                assertNotNull(result)
                assertEquals(userId, result)
            }.verifyComplete()

        val userCaptor = argumentCaptor<User>()
        verify(userRepository).save(userCaptor.capture())
        assertEquals("testuser", userCaptor.firstValue.username)
        verify(userEventPublisher).publishCreated(savedUser)
    }

    @Test
    fun `createUser should throw ConflictException when user already exists`() {
        val existingUser = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = createdAt)

        whenever(userRepository.findByUsername("testuser")).thenReturn(Mono.just(existingUser))

        StepVerifier
            .create(userService.createUser("testuser", "Test@123", null))
            .expectErrorMatches { it is ConflictException && it.message == "User with this username already exists" }
            .verify()

        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `getUser should return user when found`() {
        val user = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = createdAt)

        whenever(userRepository.findById(userId)).thenReturn(Mono.just(user))

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

        whenever(userRepository.findById(userId)).thenReturn(Mono.just(existingUser))
        whenever(userRepository.save(any<User>())).thenAnswer { Mono.just(it.arguments[0] as User) }

        StepVerifier
            .create(userService.updateUser(userId, "newname", null, null))
            .assertNext { result ->
                assertNotNull(result)
                assertEquals("newname", result.username)
            }.verifyComplete()

        val userCaptor = argumentCaptor<User>()
        verify(userRepository).save(userCaptor.capture())
        assertEquals("newname", userCaptor.firstValue.username)
        verify(userEventPublisher).publishUpdated(any())
    }

    @Test
    fun `updateUser should throw NotFoundException when user not found`() {
        whenever(userRepository.findById(userId)).thenReturn(Mono.empty())

        StepVerifier
            .create(userService.updateUser(userId, "newname", null, null))
            .expectErrorMatches { it is NotFoundException && it.message == "User not found" }
            .verify()

        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `deleteUser should delete user when exists`() {
        val user = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = createdAt)

        whenever(userRepository.findById(userId)).thenReturn(Mono.just(user))
        whenever(userRepository.deleteById(userId)).thenReturn(Mono.empty())

        StepVerifier
            .create(userService.deleteUser(userId))
            .verifyComplete()

        verify(userRepository).deleteById(userId)
        verify(userEventPublisher).publishDeleted(user)
    }

    @Test
    fun `deleteUser should throw NotFoundException when user not found`() {
        whenever(userRepository.findById(userId)).thenReturn(Mono.empty())

        StepVerifier
            .create(userService.deleteUser(userId))
            .expectErrorMatches { it is NotFoundException && it.message == "User not found" }
            .verify()

        verify(userRepository, never()).deleteById(any<UUID>())
        verify(userEventPublisher, never()).publishDeleted(any())
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
        val user = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = createdAt)
        val expiresAt = Instant.now().plusSeconds(86400)
        val tokenResult = TokenResult("mock-jwt-token", expiresAt)

        whenever(userRepository.findByUsername("testuser")).thenReturn(Mono.just(user))
        whenever(passwordEncoder.matches("Test@123", user.passwordHash)).thenReturn(true)
        whenever(tokenProvider.generateToken(user)).thenReturn(tokenResult)

        StepVerifier
            .create(userService.authenticate("testuser", "Test@123"))
            .assertNext { result ->
                assertNotNull(result)
                assertEquals("mock-jwt-token", result.token)
                assertEquals(expiresAt, result.expiresAt)
                assertEquals("testuser", result.username)
                assertEquals("USER", result.role)
            }.verifyComplete()

        verify(tokenProvider).generateToken(user)
    }

    @Test
    fun `authenticate should throw UnauthorizedException for invalid username`() {
        whenever(userRepository.findByUsername("nonexistent")).thenReturn(Mono.empty())

        StepVerifier
            .create(userService.authenticate("nonexistent", "Test@123"))
            .expectErrorMatches { it is UnauthorizedException && it.message == "Invalid username or password" }
            .verify()

        verify(passwordEncoder, never()).matches(any(), any())
        verify(tokenProvider, never()).generateToken(any())
    }

    @Test
    fun `authenticate should throw UnauthorizedException for invalid password`() {
        val user = TestEntityFactory.createUser(id = userId, username = "testuser", createdAt = createdAt)

        whenever(userRepository.findByUsername("testuser")).thenReturn(Mono.just(user))
        whenever(passwordEncoder.matches("WrongPassword", user.passwordHash)).thenReturn(false)

        StepVerifier
            .create(userService.authenticate("testuser", "WrongPassword"))
            .expectErrorMatches { it is UnauthorizedException && it.message == "Invalid username or password" }
            .verify()

        verify(tokenProvider, never()).generateToken(any())
    }
}
