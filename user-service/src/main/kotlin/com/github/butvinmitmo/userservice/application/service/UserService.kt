package com.github.butvinmitmo.userservice.application.service

import com.github.butvinmitmo.userservice.application.interfaces.provider.DivinationServiceProvider
import com.github.butvinmitmo.userservice.application.interfaces.provider.PasswordEncoder
import com.github.butvinmitmo.userservice.application.interfaces.provider.TokenProvider
import com.github.butvinmitmo.userservice.application.interfaces.repository.RoleRepository
import com.github.butvinmitmo.userservice.application.interfaces.repository.UserRepository
import com.github.butvinmitmo.userservice.domain.model.Role
import com.github.butvinmitmo.userservice.domain.model.RoleType
import com.github.butvinmitmo.userservice.domain.model.User
import com.github.butvinmitmo.userservice.exception.ConflictException
import com.github.butvinmitmo.userservice.exception.NotFoundException
import com.github.butvinmitmo.userservice.exception.UnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Instant
import java.util.UUID

data class PageResult<T>(
    val content: List<T>,
    val totalElements: Long,
)

data class AuthResult(
    val token: String,
    val expiresAt: Instant,
    val username: String,
    val role: String,
)

@Service
class UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenProvider: TokenProvider,
    private val divinationServiceProvider: DivinationServiceProvider,
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    fun authenticate(
        username: String,
        password: String,
    ): Mono<AuthResult> =
        userRepository
            .findByUsername(username)
            .switchIfEmpty(Mono.error(UnauthorizedException("Invalid username or password")))
            .flatMap { user ->
                Mono
                    .fromCallable { passwordEncoder.matches(password, user.passwordHash) }
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap { matches ->
                        if (!matches) {
                            Mono.error(UnauthorizedException("Invalid username or password"))
                        } else {
                            val tokenResult = tokenProvider.generateToken(user)
                            Mono.just(
                                AuthResult(
                                    token = tokenResult.token,
                                    expiresAt = tokenResult.expiresAt,
                                    username = user.username,
                                    role = user.role.name,
                                ),
                            )
                        }
                    }
            }

    fun createUser(
        username: String,
        password: String,
        roleName: String?,
    ): Mono<UUID> =
        userRepository
            .findByUsername(username)
            .flatMap<UUID> { Mono.error(ConflictException("User with this username already exists")) }
            .switchIfEmpty(
                Mono.defer {
                    getRoleByName(roleName).flatMap { role ->
                        Mono
                            .fromCallable { passwordEncoder.encode(password) }
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap { passwordHash ->
                                val user =
                                    User(
                                        id = null,
                                        username = username,
                                        passwordHash = passwordHash,
                                        role = role,
                                        createdAt = null,
                                    )
                                userRepository.save(user).map { saved -> saved.id!! }
                            }
                    }
                },
            )

    fun getUsers(
        page: Int,
        size: Int,
    ): Mono<PageResult<User>> {
        val offset = page.toLong() * size
        return userRepository
            .count()
            .flatMap { totalElements ->
                userRepository
                    .findAllPaginated(offset, size)
                    .collectList()
                    .map { users ->
                        PageResult(
                            content = users,
                            totalElements = totalElements,
                        )
                    }
            }
    }

    fun getUser(id: UUID): Mono<User> =
        userRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("User not found")))

    fun updateUser(
        id: UUID,
        username: String?,
        password: String?,
        roleName: String?,
    ): Mono<User> =
        userRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("User not found")))
            .flatMap { user ->
                val roleMono =
                    if (roleName != null) {
                        getRoleByName(roleName)
                    } else {
                        Mono.just(user.role)
                    }

                val passwordHashMono =
                    if (password != null) {
                        Mono
                            .fromCallable { passwordEncoder.encode(password) }
                            .subscribeOn(Schedulers.boundedElastic())
                    } else {
                        Mono.just(user.passwordHash)
                    }

                Mono
                    .zip(roleMono, passwordHashMono)
                    .flatMap { tuple ->
                        val updatedUser =
                            user.copy(
                                username = username ?: user.username,
                                passwordHash = tuple.t2,
                                role = tuple.t1,
                            )
                        userRepository.save(updatedUser)
                    }
            }

    fun deleteUser(id: UUID): Mono<Void> =
        userRepository
            .existsById(id)
            .flatMap { exists ->
                if (!exists) {
                    Mono.error(NotFoundException("User not found"))
                } else {
                    logger.info("Deleting user data in divination-service for user $id")
                    Mono
                        .fromCallable { divinationServiceProvider.deleteUserData(id) }
                        .subscribeOn(Schedulers.boundedElastic())
                        .doOnSuccess {
                            logger.info(
                                "Successfully deleted user data in divination-service for user $id",
                            )
                        }.then(Mono.defer { userRepository.deleteById(id) })
                }
            }

    private fun getRoleByName(roleName: String?): Mono<Role> {
        val effectiveRoleName = roleName ?: RoleType.USER.name
        return roleRepository
            .findByName(effectiveRoleName)
            .switchIfEmpty(Mono.error(NotFoundException("Role not found: $effectiveRoleName")))
    }
}
