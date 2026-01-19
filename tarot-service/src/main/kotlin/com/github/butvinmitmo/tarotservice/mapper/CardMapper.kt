package com.github.butvinmitmo.tarotservice.mapper

import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.tarotservice.entity.ArcanaType
import com.github.butvinmitmo.tarotservice.entity.Card
import org.springframework.stereotype.Component

@Component
class CardMapper(
    private val arcanaTypeMapper: ArcanaTypeMapper,
) {
    fun toDto(
        card: Card,
        arcanaType: ArcanaType,
    ): CardDto =
        CardDto(
            id = card.id!!,
            name = card.name,
            arcanaType = arcanaTypeMapper.toDto(arcanaType),
        )
}
