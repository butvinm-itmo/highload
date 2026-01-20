package com.github.butvinmitmo.userservice.infrastructure.persistence.repository

import com.github.butvinmitmo.userservice.infrastructure.persistence.entity.RoleEntity
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono
import java.util.UUID

interface SpringDataRoleRepository : R2dbcRepository<RoleEntity, UUID> {
    fun findByName(name: String): Mono<RoleEntity>
}
