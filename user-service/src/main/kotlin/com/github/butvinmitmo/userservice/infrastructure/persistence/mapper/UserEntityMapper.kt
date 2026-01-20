package com.github.butvinmitmo.userservice.infrastructure.persistence.mapper

import com.github.butvinmitmo.userservice.domain.model.Role
import com.github.butvinmitmo.userservice.domain.model.User
import com.github.butvinmitmo.userservice.infrastructure.persistence.entity.UserEntity
import org.springframework.stereotype.Component

@Component
class UserEntityMapper {
    fun toDomain(
        entity: UserEntity,
        role: Role,
    ): User =
        User(
            id = entity.id!!,
            username = entity.username,
            passwordHash = entity.passwordHash,
            role = role,
            createdAt = entity.createdAt!!,
        )

    fun toEntity(user: User): UserEntity =
        UserEntity(
            id = user.id,
            username = user.username,
            passwordHash = user.passwordHash,
            roleId = user.role.id,
            createdAt = user.createdAt,
        )
}
