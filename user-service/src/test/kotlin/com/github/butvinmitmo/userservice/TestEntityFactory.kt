package com.github.butvinmitmo.userservice

import com.github.butvinmitmo.userservice.domain.model.Role
import com.github.butvinmitmo.userservice.domain.model.RoleType
import com.github.butvinmitmo.userservice.domain.model.User
import com.github.butvinmitmo.userservice.infrastructure.persistence.entity.UserEntity
import java.time.Instant
import java.util.UUID

object TestEntityFactory {
    val testUserRole = Role(id = RoleType.USER_ID, name = "USER")
    val testMediumRole = Role(id = RoleType.MEDIUM_ID, name = "MEDIUM")
    val testAdminRole = Role(id = RoleType.ADMIN_ID, name = "ADMIN")

    fun createUser(
        id: UUID? = UUID.randomUUID(),
        username: String,
        passwordHash: String = "\$2a\$10\$testHashForTestingPurposesOnly",
        role: Role = testUserRole,
        createdAt: Instant? = Instant.now(),
    ): User =
        User(
            id = id,
            username = username,
            passwordHash = passwordHash,
            role = role,
            createdAt = createdAt,
        )

    fun createUserEntity(
        id: UUID? = UUID.randomUUID(),
        username: String,
        passwordHash: String = "\$2a\$10\$testHashForTestingPurposesOnly",
        roleId: UUID = RoleType.USER_ID,
        createdAt: Instant? = Instant.now(),
    ): UserEntity =
        UserEntity(
            id = id,
            username = username,
            passwordHash = passwordHash,
            roleId = roleId,
            createdAt = createdAt,
        )
}
