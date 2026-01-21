package com.github.butvinmitmo.divinationservice.infrastructure.persistence.mapper

import com.github.butvinmitmo.divinationservice.domain.model.Interpretation
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.entity.InterpretationEntity
import org.springframework.stereotype.Component

@Component
class InterpretationEntityMapper {
    fun toDomain(entity: InterpretationEntity): Interpretation =
        Interpretation(
            id = entity.id,
            text = entity.text,
            authorId = entity.authorId,
            spreadId = entity.spreadId,
            createdAt = entity.createdAt,
        )

    fun toEntity(domain: Interpretation): InterpretationEntity =
        InterpretationEntity(
            id = domain.id,
            text = domain.text,
            authorId = domain.authorId,
            spreadId = domain.spreadId,
            createdAt = domain.createdAt,
        )
}
