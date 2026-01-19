package com.github.butvinmitmo.userservice.repository

import com.github.butvinmitmo.userservice.entity.Role
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface RoleRepository : R2dbcRepository<Role, UUID> {
    fun findByName(name: String): Mono<Role>
}
