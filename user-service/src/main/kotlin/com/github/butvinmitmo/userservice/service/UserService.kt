package com.github.butvinmitmo.userservice.service

import com.github.butvinmitmo.shared.client.DivinationServiceInternalClient
import com.github.butvinmitmo.shared.dto.AuthTokenResponse
import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.CreateUserResponse
import com.github.butvinmitmo.shared.dto.LoginRequest
import com.github.butvinmitmo.shared.dto.PageResponse
import com.github.butvinmitmo.shared.dto.UpdateUserRequest
import com.github.butvinmitmo.shared.dto.UserDto
import com.github.butvinmitmo.userservice.entity.User
import com.github.butvinmitmo.userservice.exception.ConflictException
import com.github.butvinmitmo.userservice.exception.NotFoundException
import com.github.butvinmitmo.userservice.exception.UnauthorizedException
import com.github.butvinmitmo.userservice.mapper.UserMapper
import com.github.butvinmitmo.userservice.repository.UserRepository
import com.github.butvinmitmo.userservice.security.JwtUtil
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val roleService: RoleService,
    private val userMapper: UserMapper,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
    private val divinationServiceInternalClient: DivinationServiceInternalClient,
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    fun authenticate(request: LoginRequest): Mono<AuthTokenResponse> =
        userRepository
            .findByUsername(request.username)
            .switchIfEmpty(Mono.error(UnauthorizedException("Invalid username or password")))
            .flatMap { user ->
                Mono
                    .fromCallable { passwordEncoder.matches(request.password, user.passwordHash) }
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap { matches ->
                        if (!matches) {
                            Mono.error(UnauthorizedException("Invalid username or password"))
                        } else {
                            roleService.getRoleById(user.roleId).map { role ->
                                val (token, expiresAt) = jwtUtil.generateToken(user, role)
                                AuthTokenResponse(
                                    token = token,
                                    expiresAt = expiresAt,
                                    username = user.username,
                                    role = role.name,
                                )
                            }
                        }
                    }
            }

    fun createUser(request: CreateUserRequest): Mono<CreateUserResponse> =
        userRepository
            .findByUsername(request.username)
            .flatMap<CreateUserResponse> { Mono.error(ConflictException("User with this username already exists")) }
            .switchIfEmpty(
                Mono.defer {
                    roleService.getRoleByName(request.role).flatMap { role ->
                        Mono
                            .fromCallable { passwordEncoder.encode(request.password) }
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap { passwordHash ->
                                val user =
                                    User(
                                        username = request.username,
                                        passwordHash = passwordHash,
                                        roleId = role.id!!,
                                    )
                                userRepository.save(user).map { saved ->
                                    CreateUserResponse(id = saved.id!!)
                                }
                            }
                    }
                },
            )

    fun getUsers(
        page: Int,
        size: Int,
    ): Mono<PageResponse<UserDto>> {
        val offset = page.toLong() * size
        return userRepository
            .count()
            .flatMap { totalElements ->
                userRepository
                    .findAllPaginated(offset, size)
                    .flatMap { user ->
                        roleService.getRoleById(user.roleId).map { role ->
                            userMapper.toDto(user, role)
                        }
                    }.collectList()
                    .map { users ->
                        val totalPages = if (totalElements == 0L) 0 else ((totalElements - 1) / size + 1).toInt()
                        PageResponse(
                            content = users,
                            page = page,
                            size = size,
                            totalElements = totalElements,
                            totalPages = totalPages,
                            isFirst = page == 0,
                            isLast = page >= totalPages - 1,
                        )
                    }
            }
    }

    fun getUser(id: UUID): Mono<UserDto> =
        userRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("User not found")))
            .flatMap { user ->
                roleService.getRoleById(user.roleId).map { role ->
                    userMapper.toDto(user, role)
                }
            }

    fun updateUser(
        id: UUID,
        request: UpdateUserRequest,
    ): Mono<UserDto> =
        userRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("User not found")))
            .flatMap { user ->
                val roleIdMono =
                    if (request.role != null) {
                        roleService.getRoleByName(request.role).map { it.id!! }
                    } else {
                        Mono.just(user.roleId)
                    }

                val passwordHashMono =
                    if (request.password != null) {
                        Mono
                            .fromCallable { passwordEncoder.encode(request.password) }
                            .subscribeOn(Schedulers.boundedElastic())
                    } else {
                        Mono.just(user.passwordHash)
                    }

                Mono
                    .zip(roleIdMono, passwordHashMono)
                    .flatMap { tuple ->
                        val updatedUser =
                            user.copy(
                                username = request.username ?: user.username,
                                passwordHash = tuple.t2,
                                roleId = tuple.t1,
                            )
                        userRepository.save(updatedUser)
                    }.flatMap { saved ->
                        roleService.getRoleById(saved.roleId).map { role ->
                            userMapper.toDto(saved, role)
                        }
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
                        .fromCallable { divinationServiceInternalClient.deleteUserData(id) }
                        .subscribeOn(Schedulers.boundedElastic())
                        .doOnSuccess {
                            logger.info(
                                "Successfully deleted user data in divination-service for user $id",
                            )
                        }.then(Mono.defer { userRepository.deleteById(id) })
                }
            }
}
