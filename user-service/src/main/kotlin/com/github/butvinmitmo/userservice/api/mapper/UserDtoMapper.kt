package com.github.butvinmitmo.userservice.api.mapper

import com.github.butvinmitmo.shared.dto.UserDto
import com.github.butvinmitmo.userservice.domain.model.User
import org.springframework.stereotype.Component

@Component
class UserDtoMapper {
    fun toDto(user: User): UserDto =
        UserDto(
            id = user.id!!,
            username = user.username,
            role = user.role.name,
            createdAt = user.createdAt!!,
        )
}
