package com.github.butvinmitmo.divinationservice

import com.github.butvinmitmo.divinationservice.domain.model.Interpretation
import com.github.butvinmitmo.divinationservice.domain.model.Spread
import com.github.butvinmitmo.divinationservice.domain.model.SpreadCard
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.entity.InterpretationEntity
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.entity.SpreadCardEntity
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.entity.SpreadEntity
import java.time.Instant
import java.util.UUID

object TestEntityFactory {
    fun createSpread(
        id: UUID = UUID.randomUUID(),
        question: String? = "Test question",
        layoutTypeId: UUID = UUID.randomUUID(),
        authorId: UUID = UUID.randomUUID(),
        createdAt: Instant = Instant.now(),
    ): Spread =
        Spread(
            id = id,
            question = question,
            layoutTypeId = layoutTypeId,
            authorId = authorId,
            createdAt = createdAt,
        )

    fun createSpreadEntity(
        id: UUID? = UUID.randomUUID(),
        question: String? = "Test question",
        layoutTypeId: UUID = UUID.randomUUID(),
        authorId: UUID = UUID.randomUUID(),
        createdAt: Instant? = Instant.now(),
    ): SpreadEntity =
        SpreadEntity(
            id = id,
            question = question,
            layoutTypeId = layoutTypeId,
            authorId = authorId,
            createdAt = createdAt,
        )

    fun createSpreadCard(
        id: UUID = UUID.randomUUID(),
        spreadId: UUID,
        cardId: UUID = UUID.randomUUID(),
        positionInSpread: Int = 1,
        isReversed: Boolean = false,
    ): SpreadCard =
        SpreadCard(
            id = id,
            spreadId = spreadId,
            cardId = cardId,
            positionInSpread = positionInSpread,
            isReversed = isReversed,
        )

    fun createSpreadCardEntity(
        id: UUID? = UUID.randomUUID(),
        spreadId: UUID,
        cardId: UUID = UUID.randomUUID(),
        positionInSpread: Int = 1,
        isReversed: Boolean = false,
    ): SpreadCardEntity =
        SpreadCardEntity(
            id = id,
            spreadId = spreadId,
            cardId = cardId,
            positionInSpread = positionInSpread,
            isReversed = isReversed,
        )

    fun createInterpretation(
        id: UUID = UUID.randomUUID(),
        text: String = "Test interpretation",
        authorId: UUID = UUID.randomUUID(),
        spreadId: UUID,
        createdAt: Instant = Instant.now(),
    ): Interpretation =
        Interpretation(
            id = id,
            text = text,
            authorId = authorId,
            spreadId = spreadId,
            createdAt = createdAt,
        )

    fun createInterpretationEntity(
        id: UUID? = UUID.randomUUID(),
        text: String = "Test interpretation",
        authorId: UUID = UUID.randomUUID(),
        spreadId: UUID,
        createdAt: Instant? = Instant.now(),
    ): InterpretationEntity =
        InterpretationEntity(
            id = id,
            text = text,
            authorId = authorId,
            spreadId = spreadId,
            createdAt = createdAt,
        )
}
