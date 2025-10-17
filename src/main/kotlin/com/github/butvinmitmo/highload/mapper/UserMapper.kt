package com.github.butvinmitmo.highload.mapper

import com.github.butvinmitmo.highload.dto.UserDto
import com.github.butvinmitmo.highload.entity.User
import org.springframework.stereotype.Component

@Component
class UserMapper {
    fun toDto(entity: User): UserDto =
        UserDto(
            id = entity.id,
            username = entity.username,
            createdAt = entity.createdAt,
        )

    fun toDto(entities: List<User>): List<UserDto> = entities.map { toDto(it) }
}
