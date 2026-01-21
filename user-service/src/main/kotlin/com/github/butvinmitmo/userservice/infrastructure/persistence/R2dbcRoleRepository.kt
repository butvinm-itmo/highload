package com.github.butvinmitmo.userservice.infrastructure.persistence

import com.github.butvinmitmo.userservice.application.interfaces.repository.RoleRepository
import com.github.butvinmitmo.userservice.domain.model.Role
import com.github.butvinmitmo.userservice.infrastructure.persistence.mapper.RoleEntityMapper
import com.github.butvinmitmo.userservice.infrastructure.persistence.repository.SpringDataRoleRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class R2dbcRoleRepository(
    private val springDataRoleRepository: SpringDataRoleRepository,
    private val roleEntityMapper: RoleEntityMapper,
) : RoleRepository {
    override fun findById(id: UUID): Mono<Role> =
        springDataRoleRepository
            .findById(id)
            .map { roleEntityMapper.toDomain(it) }

    override fun findByName(name: String): Mono<Role> =
        springDataRoleRepository
            .findByName(name)
            .map { roleEntityMapper.toDomain(it) }
}
