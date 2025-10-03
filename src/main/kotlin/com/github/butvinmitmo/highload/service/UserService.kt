package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.dto.CreateUserRequest
import com.github.butvinmitmo.highload.dto.UpdateUserRequest
import com.github.butvinmitmo.highload.dto.UserDto
import com.github.butvinmitmo.highload.entity.User
import com.github.butvinmitmo.highload.exception.ConflictException
import com.github.butvinmitmo.highload.exception.NotFoundException
import com.github.butvinmitmo.highload.mapper.UserMapper
import com.github.butvinmitmo.highload.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
) {
    @Transactional
    fun createUser(request: CreateUserRequest): UserDto {
        // Check if user with this ID already exists
        if (userRepository.existsById(request.id)) {
            throw ConflictException("User with this ID already exists")
        }

        val user =
            User(
                id = request.id,
                username = request.username ?: "user_${request.id}",
            )

        val saved = userRepository.save(user)
        return userMapper.toDto(saved)
    }

    fun getUsers(
        page: Int,
        size: Int,
    ): List<UserDto> {
        val pageable = PageRequest.of(page, size)
        return userRepository
            .findAll(pageable)
            .content
            .map { userMapper.toDto(it) }
    }

    fun getUser(id: UUID): UserDto {
        val user =
            userRepository.findById(id)
                .orElseThrow { NotFoundException("User not found") }

        return userMapper.toDto(user)
    }

    @Transactional
    fun updateUser(
        id: UUID,
        request: UpdateUserRequest,
    ): UserDto {
        val user =
            userRepository.findById(id)
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

        // Database CASCADE DELETE handles all related deletions automatically:
        // - User's spreads (which cascades to interpretations and spread_cards)
        // - User's interpretations on other spreads
        userRepository.deleteById(id)
    }
}
