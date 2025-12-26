package com.github.butvinmitmo.divinationservice.mapper

import com.github.butvinmitmo.divinationservice.entity.Interpretation
import com.github.butvinmitmo.divinationservice.entity.Spread
import com.github.butvinmitmo.divinationservice.entity.SpreadCard
import com.github.butvinmitmo.shared.client.TarotServiceClient
import com.github.butvinmitmo.shared.client.UserServiceClient
import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.InterpretationDto
import com.github.butvinmitmo.shared.dto.LayoutTypeDto
import com.github.butvinmitmo.shared.dto.SpreadCardDto
import com.github.butvinmitmo.shared.dto.SpreadDto
import com.github.butvinmitmo.shared.dto.SpreadSummaryDto
import com.github.butvinmitmo.shared.dto.UserDto
import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class SpreadMapper(
    private val userServiceClient: UserServiceClient,
    private val tarotServiceClient: TarotServiceClient,
) {
    private val logger = LoggerFactory.getLogger(SpreadMapper::class.java)

    fun toDto(
        spread: Spread,
        spreadCards: List<SpreadCard>,
        interpretations: List<Interpretation>,
        cardCache: Map<UUID, CardDto> = emptyMap(),
    ): SpreadDto {
        val author = getUserOrPlaceholder(spread.authorId)
        val layoutType = getLayoutTypeOrPlaceholder(spread.layoutTypeId)

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
            interpretations =
                interpretations.map { interpretation ->
                    val interpAuthor = getUserOrPlaceholder(interpretation.authorId)
                    InterpretationDto(
                        id = interpretation.id!!,
                        text = interpretation.text,
                        author = interpAuthor,
                        spreadId = interpretation.spreadId,
                        createdAt = interpretation.createdAt!!,
                    )
                },
            author = author,
            createdAt = spread.createdAt!!,
        )
    }

    fun toSummaryDto(
        spread: Spread,
        interpretationsCount: Int = 0,
    ): SpreadSummaryDto {
        val author = getUserOrPlaceholder(spread.authorId)
        val layoutType = getLayoutTypeOrPlaceholder(spread.layoutTypeId)

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

    /**
     * Fetches user from user-service. Returns placeholder if user was deleted.
     */
    private fun getUserOrPlaceholder(userId: UUID): UserDto =
        try {
            userServiceClient.getUserById(userId).body!!
        } catch (e: FeignException.NotFound) {
            logger.warn("User {} not found, returning placeholder", userId)
            createDeletedUserPlaceholder(userId)
        }

    /**
     * Fetches layout type from tarot-service. Returns placeholder if not found.
     */
    private fun getLayoutTypeOrPlaceholder(layoutTypeId: UUID): LayoutTypeDto =
        try {
            tarotServiceClient.getLayoutTypeById(layoutTypeId).body!!
        } catch (e: FeignException.NotFound) {
            logger.warn("LayoutType {} not found, returning placeholder", layoutTypeId)
            createDeletedLayoutTypePlaceholder(layoutTypeId)
        }

    private fun createDeletedUserPlaceholder(userId: UUID): UserDto =
        UserDto(
            id = userId,
            username = "[Deleted User]",
            createdAt = Instant.EPOCH,
        )

    private fun createDeletedLayoutTypePlaceholder(layoutTypeId: UUID): LayoutTypeDto =
        LayoutTypeDto(
            id = layoutTypeId,
            name = "[Deleted Layout]",
            cardsCount = 0,
        )

    private fun fetchCard(cardId: UUID): CardDto {
        // For single card fetches, we make individual calls
        // In production, you might want to add a batch endpoint
        val cards = tarotServiceClient.getRandomCards(1).body!!
        return cards.firstOrNull()
            ?: throw IllegalStateException("Could not fetch card with id $cardId")
    }
}
