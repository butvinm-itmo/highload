package com.github.butvinmitmo.highload.mapper

import com.github.butvinmitmo.highload.dto.CardDto
import com.github.butvinmitmo.highload.dto.CardSummaryDto
import com.github.butvinmitmo.highload.entity.Card
import org.springframework.stereotype.Component

@Component
class CardMapper(
    private val arcanaTypeMapper: ArcanaTypeMapper,
) {
    fun toDto(card: Card): CardDto =
        CardDto(
            id = card.id!!,
            name = card.name,
            arcanaType = arcanaTypeMapper.toDto(card.arcanaType),
        )

    fun toSummaryDto(card: Card): CardSummaryDto =
        CardSummaryDto(
            id = card.id!!,
            name = card.name,
            arcanaTypeName = card.arcanaType.name,
        )
}
