package com.github.butvinmitmo.userservice.service

import com.github.butvinmitmo.userservice.entity.Role
import com.github.butvinmitmo.userservice.entity.RoleType
import com.github.butvinmitmo.userservice.exception.NotFoundException
import com.github.butvinmitmo.userservice.repository.RoleRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class RoleService(
    private val roleRepository: RoleRepository,
) {
    fun getRoleByName(roleName: String?): Mono<Role> {
        val effectiveRoleName = roleName ?: RoleType.USER.name
        return roleRepository
            .findByName(effectiveRoleName)
            .switchIfEmpty(Mono.error(NotFoundException("Role not found: $effectiveRoleName")))
    }

    fun getRoleByType(roleType: RoleType): Mono<Role> =
        roleRepository
            .findByName(roleType.name)
            .switchIfEmpty(Mono.error(NotFoundException("Role not found: ${roleType.name}")))

    fun getRoleById(roleId: UUID): Mono<Role> =
        roleRepository
            .findById(roleId)
            .switchIfEmpty(Mono.error(NotFoundException("Role not found: $roleId")))
}
