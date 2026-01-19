package com.github.butvinmitmo.userservice

import com.github.butvinmitmo.userservice.entity.Role
import com.github.butvinmitmo.userservice.entity.RoleType
import com.github.butvinmitmo.userservice.entity.User
import java.time.Instant
import java.util.UUID

object TestEntityFactory {
    val testUserRole = Role(id = RoleType.USER_ID, name = "USER")
    val testMediumRole = Role(id = RoleType.MEDIUM_ID, name = "MEDIUM")
    val testAdminRole = Role(id = RoleType.ADMIN_ID, name = "ADMIN")

    fun createUser(
        id: UUID = UUID.randomUUID(),
        username: String,
        passwordHash: String = "\$2a\$10\$testHashForTestingPurposesOnly",
        roleId: UUID = RoleType.USER_ID,
        createdAt: Instant = Instant.now(),
    ): User =
        User(
            id = id,
            username = username,
            passwordHash = passwordHash,
            roleId = roleId,
            createdAt = createdAt,
        )
}
