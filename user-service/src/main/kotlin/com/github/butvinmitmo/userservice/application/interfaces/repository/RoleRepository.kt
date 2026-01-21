package com.github.butvinmitmo.userservice.application.interfaces.repository

import com.github.butvinmitmo.userservice.domain.model.Role
import reactor.core.publisher.Mono
import java.util.UUID

interface RoleRepository {
    fun findById(id: UUID): Mono<Role>

    fun findByName(name: String): Mono<Role>
}
