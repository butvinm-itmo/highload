package com.github.butvinmitmo.userservice.infrastructure.persistence.mapper

import com.github.butvinmitmo.userservice.domain.model.Role
import com.github.butvinmitmo.userservice.infrastructure.persistence.entity.RoleEntity
import org.springframework.stereotype.Component

@Component
class RoleEntityMapper {
    fun toDomain(entity: RoleEntity): Role =
        Role(
            id = entity.id!!,
            name = entity.name,
        )
}
