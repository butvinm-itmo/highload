package com.github.butvinmitmo.divinationservice.infrastructure.persistence.mapper

import com.github.butvinmitmo.divinationservice.domain.model.Spread
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.entity.SpreadEntity
import org.springframework.stereotype.Component

@Component
class SpreadEntityMapper {
    fun toDomain(entity: SpreadEntity): Spread =
        Spread(
            id = entity.id,
            question = entity.question,
            layoutTypeId = entity.layoutTypeId,
            authorId = entity.authorId,
            createdAt = entity.createdAt,
        )

    fun toEntity(domain: Spread): SpreadEntity =
        SpreadEntity(
            id = domain.id,
            question = domain.question,
            layoutTypeId = domain.layoutTypeId,
            authorId = domain.authorId,
            createdAt = domain.createdAt,
        )
}
