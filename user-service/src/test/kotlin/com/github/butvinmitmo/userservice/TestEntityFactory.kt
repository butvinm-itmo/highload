package com.github.butvinmitmo.userservice

import com.github.butvinmitmo.userservice.entity.User
import java.time.Instant
import java.util.UUID

object TestEntityFactory {
    fun createUser(
        id: UUID,
        username: String,
        createdAt: Instant = Instant.now(),
    ): User {
        val user = User(username = username)
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
