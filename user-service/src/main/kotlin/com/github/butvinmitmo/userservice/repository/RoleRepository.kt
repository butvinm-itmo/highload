package com.github.butvinmitmo.userservice.repository

import com.github.butvinmitmo.userservice.entity.Role
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RoleRepository : JpaRepository<Role, UUID> {
    fun findByName(name: String): Role?
}
