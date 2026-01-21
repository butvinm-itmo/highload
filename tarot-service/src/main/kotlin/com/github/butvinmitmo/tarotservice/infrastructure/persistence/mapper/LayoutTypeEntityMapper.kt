package com.github.butvinmitmo.tarotservice.infrastructure.persistence.mapper

import com.github.butvinmitmo.tarotservice.domain.model.LayoutType
import com.github.butvinmitmo.tarotservice.infrastructure.persistence.entity.LayoutTypeEntity
import org.springframework.stereotype.Component

@Component
class LayoutTypeEntityMapper {
    fun toDomain(entity: LayoutTypeEntity): LayoutType =
        LayoutType(
            id = entity.id!!,
            name = entity.name,
            cardsCount = entity.cardsCount,
        )
}
