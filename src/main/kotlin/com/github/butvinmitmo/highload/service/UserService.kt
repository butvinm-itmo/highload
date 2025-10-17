package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.dto.CreateUserRequest
import com.github.butvinmitmo.highload.dto.CreateUserResponse
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
    ): List<UserDto> {
        val pageable = PageRequest.of(page, size)
        return userRepository
            .findAll(pageable)
            .content
            .map { userMapper.toDto(it) }
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

        userRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun getUserEntity(id: UUID): User =
        userRepository
            .findById(id)
            .orElseThrow { NotFoundException("User not found") }
}
