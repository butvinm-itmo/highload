package com.github.butvinmitmo.divinationservice.mapper

import com.github.butvinmitmo.divinationservice.entity.Interpretation
import com.github.butvinmitmo.divinationservice.entity.Spread
import com.github.butvinmitmo.divinationservice.entity.SpreadCard
import com.github.butvinmitmo.shared.client.TarotServiceClient
import com.github.butvinmitmo.shared.client.UserServiceClient
import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.SpreadCardDto
import com.github.butvinmitmo.shared.dto.SpreadDto
import com.github.butvinmitmo.shared.dto.SpreadSummaryDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SpreadMapper(
    private val userServiceClient: UserServiceClient,
    private val tarotServiceClient: TarotServiceClient,
    private val interpretationMapper: InterpretationMapper,
) {
    // System context for internal service-to-service calls
    private val systemUserId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private val systemRole = "SYSTEM"

    fun toDto(
        spread: Spread,
        spreadCards: List<SpreadCard>,
        interpretations: List<Interpretation>,
        cardCache: Map<UUID, CardDto> = emptyMap(),
    ): SpreadDto {
        val author = userServiceClient.getUserById(systemUserId, systemRole, spread.authorId).body!!
        val layoutType = tarotServiceClient.getLayoutTypeById(systemUserId, systemRole, spread.layoutTypeId).body!!

        return SpreadDto(
            id = spread.id!!,
            question = spread.question,
            layoutType = layoutType,
            cards =
                spreadCards.sortedBy { it.positionInSpread }.map { spreadCard ->
                    val card = cardCache[spreadCard.cardId] ?: fetchCard(spreadCard.cardId)
                    SpreadCardDto(
                        id = spreadCard.id!!,
                        card = card,
                        positionInSpread = spreadCard.positionInSpread,
                        isReversed = spreadCard.isReversed,
                    )
                },
            interpretations = interpretations.map { interpretationMapper.toDto(it) },
            author = author,
            createdAt = spread.createdAt!!,
        )
    }

    fun toSummaryDto(
        spread: Spread,
        interpretationsCount: Int = 0,
    ): SpreadSummaryDto {
        val author = userServiceClient.getUserById(systemUserId, systemRole, spread.authorId).body!!
        val layoutType = tarotServiceClient.getLayoutTypeById(systemUserId, systemRole, spread.layoutTypeId).body!!

        return SpreadSummaryDto(
            id = spread.id!!,
            question = spread.question,
            layoutTypeName = layoutType.name,
            cardsCount = layoutType.cardsCount,
            interpretationsCount = interpretationsCount,
            authorUsername = author.username,
            createdAt = spread.createdAt!!,
        )
    }

    private fun fetchCard(cardId: UUID): CardDto {
        val allCards = mutableListOf<CardDto>()
        val pageSize = 50
        var page = 0
        var fetched: List<CardDto>
        do {
            fetched = tarotServiceClient.getCards(systemUserId, systemRole, page, pageSize).body!!
            allCards.addAll(fetched)
            page++
        } while (fetched.size == pageSize)
        return allCards.find { it.id == cardId }
            ?: throw IllegalStateException("Could not fetch card with id $cardId")
    }
}
