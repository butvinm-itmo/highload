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
        return Spread(
            id = id,
            question = question,
            layoutTypeId = layoutTypeId,
            authorId = authorId,
            createdAt = createdAt,
        )
    }

    fun createSpreadCard(
        id: UUID = UUID.randomUUID(),
        spreadId: UUID,
        cardId: UUID = UUID.randomUUID(),
        positionInSpread: Int = 1,
        isReversed: Boolean = false,
    ): SpreadCard {
        return SpreadCard(
            id = id,
            spreadId = spreadId,
            cardId = cardId,
            positionInSpread = positionInSpread,
            isReversed = isReversed,
        )
    }

    fun createInterpretation(
        id: UUID = UUID.randomUUID(),
        text: String = "Test interpretation",
        authorId: UUID = UUID.randomUUID(),
        spreadId: UUID,
        createdAt: Instant = Instant.now(),
    ): Interpretation {
        return Interpretation(
            id = id,
            text = text,
            authorId = authorId,
            spreadId = spreadId,
            createdAt = createdAt,
        )
    }
}
