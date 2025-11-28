package com.github.butvinmitmo.userservice.mapper

import com.github.butvinmitmo.shared.dto.UserDto
import com.github.butvinmitmo.userservice.entity.User
import org.springframework.stereotype.Component

@Component
class UserMapper {
    fun toDto(entity: User): UserDto =
        UserDto(
            id = entity.id,
            username = entity.username,
            createdAt = entity.createdAt,
        )
}
