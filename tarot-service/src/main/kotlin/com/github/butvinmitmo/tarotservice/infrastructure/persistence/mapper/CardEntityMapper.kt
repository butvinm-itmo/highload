package com.github.butvinmitmo.tarotservice.infrastructure.persistence.mapper

import com.github.butvinmitmo.tarotservice.domain.model.ArcanaType
import com.github.butvinmitmo.tarotservice.domain.model.Card
import com.github.butvinmitmo.tarotservice.infrastructure.persistence.entity.CardEntity
import org.springframework.stereotype.Component

@Component
class CardEntityMapper {
    fun toDomain(
        entity: CardEntity,
        arcanaType: ArcanaType,
    ): Card =
        Card(
            id = entity.id!!,
            name = entity.name,
            arcanaType = arcanaType,
        )
}
