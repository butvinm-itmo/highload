package com.github.butvinmitmo.userservice.service

import com.github.butvinmitmo.userservice.entity.Role
import com.github.butvinmitmo.userservice.entity.RoleType
import com.github.butvinmitmo.userservice.exception.NotFoundException
import com.github.butvinmitmo.userservice.repository.RoleRepository
import org.springframework.stereotype.Service

@Service
class RoleService(
    private val roleRepository: RoleRepository,
) {
    /**
     * Get role by name. Defaults to USER role if roleName is null.
     * @param roleName The name of the role (USER, MEDIUM, or ADMIN), or null for default USER role
     * @return Role entity
     * @throws NotFoundException if role does not exist
     */
    fun getRoleByName(roleName: String?): Role {
        val effectiveRoleName = roleName ?: RoleType.USER.name
        return roleRepository.findByName(effectiveRoleName)
            ?: throw NotFoundException("Role not found: $effectiveRoleName")
    }

    /**
     * Get role by RoleType enum.
     * @param roleType The RoleType enum value
     * @return Role entity
     * @throws NotFoundException if role does not exist
     */
    fun getRoleByType(roleType: RoleType): Role =
        roleRepository.findByName(roleType.name)
            ?: throw NotFoundException("Role not found: ${roleType.name}")
}
