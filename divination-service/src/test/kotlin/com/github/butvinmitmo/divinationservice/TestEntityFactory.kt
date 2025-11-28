package com.github.butvinmitmo.divinationservice

import com.github.butvinmitmo.divinationservice.entity.Interpretation
import com.github.butvinmitmo.divinationservice.entity.Spread
import com.github.butvinmitmo.divinationservice.entity.SpreadCard
import java.time.Instant
import java.util.UUID

object TestEntityFactory {
    fun createSpread(
        id: UUID = UUID.randomUUID(),
        question: String? = "Test question",
        layoutTypeId: UUID = UUID.randomUUID(),
        authorId: UUID = UUID.randomUUID(),
        createdAt: Instant = Instant.now(),
    ): Spread {
        val spread = Spread(question = question, layoutTypeId = layoutTypeId, authorId = authorId)
        setPrivateField(spread, "id", id)
        setPrivateField(spread, "createdAt", createdAt)
        return spread
    }

    fun createSpreadCard(
        id: UUID = UUID.randomUUID(),
        spread: Spread,
        cardId: UUID = UUID.randomUUID(),
        positionInSpread: Int = 1,
        isReversed: Boolean = false,
    ): SpreadCard {
        val spreadCard =
            SpreadCard(
                spread = spread,
                cardId = cardId,
                positionInSpread = positionInSpread,
                isReversed = isReversed,
            )
        setPrivateField(spreadCard, "id", id)
        return spreadCard
    }

    fun createInterpretation(
        id: UUID = UUID.randomUUID(),
        text: String = "Test interpretation",
        authorId: UUID = UUID.randomUUID(),
        spread: Spread,
        createdAt: Instant = Instant.now(),
    ): Interpretation {
        val interpretation = Interpretation(text = text, authorId = authorId, spread = spread)
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
