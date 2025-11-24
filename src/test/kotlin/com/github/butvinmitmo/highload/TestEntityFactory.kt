package com.github.butvinmitmo.highload

import com.github.butvinmitmo.highload.entity.Interpretation
import com.github.butvinmitmo.highload.entity.LayoutType
import com.github.butvinmitmo.highload.entity.Spread
import com.github.butvinmitmo.highload.entity.User
import java.time.Instant
import java.util.UUID

/**
 * Test utility for creating entity objects with reflection-based field injection.
 *
 * Entities use @Generated fields for id and createdAt, which are populated by the database.
 * In unit tests with mocked repositories, we need to manually set these fields using reflection.
 */
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

    fun createSpread(
        id: UUID,
        question: String?,
        author: User,
        layoutType: LayoutType,
        createdAt: Instant = Instant.now(),
    ): Spread {
        val spread = Spread(question = question, author = author, layoutType = layoutType)
        setPrivateField(spread, "id", id)
        setPrivateField(spread, "createdAt", createdAt)
        return spread
    }

    fun createInterpretation(
        id: UUID,
        text: String,
        author: User,
        spread: Spread,
        createdAt: Instant = Instant.now(),
    ): Interpretation {
        val interpretation = Interpretation(text = text, author = author, spread = spread)
        setPrivateField(interpretation, "id", id)
        setPrivateField(interpretation, "createdAt", createdAt)
        return interpretation
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
