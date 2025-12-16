package com.github.butvinmitmo.userservice

import com.github.butvinmitmo.userservice.entity.Role
import com.github.butvinmitmo.userservice.entity.RoleType
import com.github.butvinmitmo.userservice.entity.User
import java.time.Instant
import java.util.UUID

object TestEntityFactory {
    private val testUserRole = Role(id = RoleType.USER_ID, name = "USER")
    private val testAdminRole = Role(id = RoleType.ADMIN_ID, name = "ADMIN")

    fun createUser(
        id: UUID,
        username: String,
        passwordHash: String = "\$2a\$10\$testHashForTestingPurposesOnly",
        role: Role = testUserRole,
        createdAt: Instant = Instant.now(),
    ): User {
        val user = User(username = username, passwordHash = passwordHash, role = role)
        setPrivateField(user, "id", id)
        setPrivateField(user, "createdAt", createdAt)
        return user
    }

    private fun setPrivateField(
        obj: Any,
        fieldName: String,
        value: Any,
    ) {
        val field = obj::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(obj, value)
    }
}
