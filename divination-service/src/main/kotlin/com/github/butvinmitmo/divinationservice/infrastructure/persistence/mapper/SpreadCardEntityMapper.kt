package com.github.butvinmitmo.divinationservice.infrastructure.persistence.mapper

import com.github.butvinmitmo.divinationservice.domain.model.SpreadCard
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.entity.SpreadCardEntity
import org.springframework.stereotype.Component

@Component
class SpreadCardEntityMapper {
    fun toDomain(entity: SpreadCardEntity): SpreadCard =
        SpreadCard(
            id = entity.id,
            spreadId = entity.spreadId,
            cardId = entity.cardId,
            positionInSpread = entity.positionInSpread,
            isReversed = entity.isReversed,
        )

    fun toEntity(domain: SpreadCard): SpreadCardEntity =
        SpreadCardEntity(
            id = domain.id,
            spreadId = domain.spreadId,
            cardId = domain.cardId,
            positionInSpread = domain.positionInSpread,
            isReversed = domain.isReversed,
        )
}
