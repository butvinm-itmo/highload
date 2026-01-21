package com.github.butvinmitmo.tarotservice.api.mapper

import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.tarotservice.domain.model.Card
import org.springframework.stereotype.Component

@Component
class CardDtoMapper(
    private val arcanaTypeDtoMapper: ArcanaTypeDtoMapper,
) {
    fun toDto(card: Card): CardDto =
        CardDto(
            id = card.id,
            name = card.name,
            arcanaType = arcanaTypeDtoMapper.toDto(card.arcanaType),
        )
}
