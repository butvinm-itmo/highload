package com.github.butvinmitmo.tarotservice.infrastructure.persistence.mapper

import com.github.butvinmitmo.tarotservice.domain.model.ArcanaType
import com.github.butvinmitmo.tarotservice.infrastructure.persistence.entity.ArcanaTypeEntity
import org.springframework.stereotype.Component

@Component
class ArcanaTypeEntityMapper {
    fun toDomain(entity: ArcanaTypeEntity): ArcanaType =
        ArcanaType(
            id = entity.id!!,
            name = entity.name,
        )
}
