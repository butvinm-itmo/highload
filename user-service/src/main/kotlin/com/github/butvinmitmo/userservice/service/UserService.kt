package com.github.butvinmitmo.userservice.service

import com.github.butvinmitmo.shared.client.DivinationServiceClient
import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.CreateUserResponse
import com.github.butvinmitmo.shared.dto.PageResponse
import com.github.butvinmitmo.shared.dto.UpdateUserRequest
import com.github.butvinmitmo.shared.dto.UserDto
import com.github.butvinmitmo.userservice.entity.User
import com.github.butvinmitmo.userservice.exception.ConflictException
import com.github.butvinmitmo.userservice.exception.NotFoundException
import com.github.butvinmitmo.userservice.mapper.UserMapper
import com.github.butvinmitmo.userservice.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
    private val divinationServiceClient: DivinationServiceClient,
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    @Transactional
    fun createUser(request: CreateUserRequest): CreateUserResponse {
        if (userRepository.findByUsername(request.username) != null) {
            throw ConflictException("User with this username already exists")
        }

        val user =
            User(
                username = request.username,
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

        val updated = userRepository.save(user)
        return userMapper.toDto(updated)
    }

    @Transactional
    fun deleteUser(id: UUID) {
        if (!userRepository.existsById(id)) {
            throw NotFoundException("User not found")
        }

        // Delete user's data in divination-service first (replaces FK CASCADE behavior)
        try {
            divinationServiceClient.deleteUserData(id)
            logger.info("Successfully deleted divination data for user {}", id)
        } catch (e: Exception) {
            logger.warn("Failed to delete divination data for user {}: {}", id, e.message)
            // Continue with user deletion even if divination cleanup fails
            // Orphaned data will be handled gracefully by divination-service
        }

        userRepository.deleteById(id)
    }

    fun getUserEntity(id: UUID): User =
        userRepository
            .findById(id)
            .orElseThrow { NotFoundException("User not found") }
}
