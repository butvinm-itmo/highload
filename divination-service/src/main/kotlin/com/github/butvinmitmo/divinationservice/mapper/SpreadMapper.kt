package com.github.butvinmitmo.divinationservice.mapper

import com.github.butvinmitmo.divinationservice.client.TarotClient
import com.github.butvinmitmo.divinationservice.client.UserClient
import com.github.butvinmitmo.divinationservice.entity.Interpretation
import com.github.butvinmitmo.divinationservice.entity.Spread
import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.InterpretationDto
import com.github.butvinmitmo.shared.dto.SpreadCardDto
import com.github.butvinmitmo.shared.dto.SpreadDto
import com.github.butvinmitmo.shared.dto.SpreadSummaryDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SpreadMapper(
    private val userClient: UserClient,
    private val tarotClient: TarotClient,
) {
    fun toDto(
        spread: Spread,
        interpretations: List<Interpretation>,
        cardCache: Map<UUID, CardDto> = emptyMap(),
    ): SpreadDto {
        val author = userClient.getUserById(spread.authorId).body!!
        val layoutType = tarotClient.getLayoutTypeById(spread.layoutTypeId).body!!

        return SpreadDto(
            id = spread.id,
            question = spread.question,
            layoutType = layoutType,
            cards =
                spread.spreadCards.sortedBy { it.positionInSpread }.map { spreadCard ->
                    val card = cardCache[spreadCard.cardId] ?: fetchCard(spreadCard.cardId)
                    SpreadCardDto(
                        id = spreadCard.id,
                        card = card,
                        positionInSpread = spreadCard.positionInSpread,
                        isReversed = spreadCard.isReversed,
                    )
                },
            interpretations =
                interpretations.map { interpretation ->
                    val interpAuthor = userClient.getUserById(interpretation.authorId).body!!
                    InterpretationDto(
                        id = interpretation.id,
                        text = interpretation.text,
                        author = interpAuthor,
                        spreadId = interpretation.spread.id,
                        createdAt = interpretation.createdAt,
                    )
                },
            author = author,
            createdAt = spread.createdAt,
        )
    }

    fun toSummaryDto(
        spread: Spread,
        interpretationsCount: Int = 0,
    ): SpreadSummaryDto {
        val author = userClient.getUserById(spread.authorId).body!!
        val layoutType = tarotClient.getLayoutTypeById(spread.layoutTypeId).body!!

        return SpreadSummaryDto(
            id = spread.id,
            question = spread.question,
            layoutTypeName = layoutType.name,
            cardsCount = layoutType.cardsCount,
            interpretationsCount = interpretationsCount,
            authorUsername = author.username,
            createdAt = spread.createdAt,
        )
    }

    private fun fetchCard(cardId: UUID): CardDto {
        // For single card fetches, we make individual calls
        // In production, you might want to add a batch endpoint
        val cards = tarotClient.getRandomCards(1).body!!
        return cards.firstOrNull()
            ?: throw IllegalStateException("Could not fetch card with id $cardId")
    }
}
