package com.github.butvinmitmo.highload.mapper

import com.github.butvinmitmo.highload.dto.UserDto
import com.github.butvinmitmo.highload.entity.User

object UserMapper {
    fun toDto(entity: User): UserDto =
        UserDto(
            id = entity.id!!,
            username = entity.username,
            createdAt = entity.createdAt,
        )

    fun toDto(entities: List<User>): List<UserDto> = entities.map { toDto(it) }
}
