package com.github.butvinmitmo.userservice.service

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
import com.github.butvinmitmo.userservice.repository.RoleRepository
import com.github.butvinmitmo.userservice.repository.UserRepository
import com.github.butvinmitmo.userservice.security.JwtUtil
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val roleService: RoleService,
    private val userMapper: UserMapper,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
) {
    @Transactional(readOnly = true)
    fun authenticate(request: LoginRequest): AuthTokenResponse {
        val user =
            userRepository.findByUsername(request.username)
                ?: throw UnauthorizedException("Invalid username or password")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw UnauthorizedException("Invalid username or password")
        }

        val (token, expiresAt) = jwtUtil.generateToken(user)

        return AuthTokenResponse(
            token = token,
            expiresAt = expiresAt,
            username = user.username,
            role = user.role.name,
        )
    }

    @Transactional
    fun createUser(request: CreateUserRequest): CreateUserResponse {
        if (userRepository.findByUsername(request.username) != null) {
            throw ConflictException("User with this username already exists")
        }

        val userRole = roleService.getRoleByName(request.role)

        val user =
            User(
                username = request.username,
                passwordHash = passwordEncoder.encode(request.password),
                role = userRole,
            )

        val saved = userRepository.save(user)
        return CreateUserResponse(id = saved.id)
    }

    fun getUsers(
        page: Int,
        size: Int,
    ): PageResponse<UserDto> {
        val pageable = PageRequest.of(page, size)
        val usersPage = userRepository.findAll(pageable)
        return PageResponse(
            content = usersPage.content.map { userMapper.toDto(it) },
            page = usersPage.number,
            size = usersPage.size,
            totalElements = usersPage.totalElements,
            totalPages = usersPage.totalPages,
            isFirst = usersPage.isFirst,
            isLast = usersPage.isLast,
        )
    }

    fun getUser(id: UUID): UserDto {
        val user =
            userRepository
                .findById(id)
                .orElseThrow { NotFoundException("User not found") }

        return userMapper.toDto(user)
    }

    @Transactional
    fun updateUser(
        id: UUID,
        request: UpdateUserRequest,
    ): UserDto {
        val user =
            userRepository
                .findById(id)
                .orElseThrow { NotFoundException("User not found") }

        request.username?.let { user.username = it }
        request.password?.let { user.passwordHash = passwordEncoder.encode(it) }
        request.role?.let { user.role = roleService.getRoleByName(it) }

        val updated = userRepository.save(user)
        return userMapper.toDto(updated)
    }

    @Transactional
    fun deleteUser(id: UUID) {
        if (!userRepository.existsById(id)) {
            throw NotFoundException("User not found")
        }

        userRepository.deleteById(id)
    }
}
