package com.github.butvinmitmo.highload.mapper

import com.github.butvinmitmo.highload.dto.ArcanaTypeDto
import com.github.butvinmitmo.highload.dto.CardDto
import com.github.butvinmitmo.highload.dto.InterpretationDto
import com.github.butvinmitmo.highload.dto.LayoutTypeDto
import com.github.butvinmitmo.highload.dto.SpreadCardDto
import com.github.butvinmitmo.highload.dto.SpreadDto
import com.github.butvinmitmo.highload.dto.SpreadSummaryDto
import com.github.butvinmitmo.highload.dto.UserDto
import com.github.butvinmitmo.highload.entity.Interpretation
import com.github.butvinmitmo.highload.entity.Spread
import com.github.butvinmitmo.highload.entity.SpreadCard
import org.springframework.stereotype.Component

@Component
class SpreadMapper {
    fun toDto(
        spread: Spread,
        spreadCards: List<SpreadCard> = emptyList(),
        interpretations: List<Interpretation> = emptyList(),
    ): SpreadDto =
        SpreadDto(
            id = spread.id!!,
            question = spread.question,
            layoutType =
                LayoutTypeDto(
                    id = spread.layoutType.id!!,
                    name = spread.layoutType.name,
                    cardsCount = spread.layoutType.cardsCount,
                ),
            cards =
                spreadCards.sortedBy { it.positionInSpread }.map { spreadCard ->
                    SpreadCardDto(
                        id = spreadCard.id!!,
                        card =
                            CardDto(
                                id = spreadCard.card.id!!,
                                name = spreadCard.card.name,
                                arcanaType =
                                    ArcanaTypeDto(
                                        id = spreadCard.card.arcanaType.id!!,
                                        name = spreadCard.card.arcanaType.name,
                                    ),
                            ),
                        positionInSpread = spreadCard.positionInSpread,
                        isReversed = spreadCard.isReversed,
                    )
                },
            interpretations =
                interpretations.map { interpretation ->
                    InterpretationDto(
                        id = interpretation.id!!,
                        text = interpretation.text,
                        author =
                            UserDto(
                                id = interpretation.author.id!!,
                                username = interpretation.author.username,
                                createdAt = interpretation.author.createdAt,
                            ),
                        spreadId = interpretation.spread.id!!,
                        createdAt = interpretation.createdAt,
                    )
                },
            author =
                UserDto(
                    id = spread.author.id!!,
                    username = spread.author.username,
                    createdAt = spread.author.createdAt,
                ),
            createdAt = spread.createdAt,
        )

    fun toSummaryDto(
        spread: Spread,
        interpretationsCount: Int = 0,
    ): SpreadSummaryDto =
        SpreadSummaryDto(
            id = spread.id!!,
            question = spread.question,
            layoutTypeName = spread.layoutType.name,
            cardsCount = spread.layoutType.cardsCount,
            interpretationsCount = interpretationsCount,
            authorUsername = spread.author.username,
            createdAt = spread.createdAt,
        )
}
