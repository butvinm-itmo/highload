package com.github.butvinmitmo.divinationservice.mapper

import com.github.butvinmitmo.divinationservice.entity.Interpretation
import com.github.butvinmitmo.divinationservice.entity.Spread
import com.github.butvinmitmo.divinationservice.entity.SpreadCard
import com.github.butvinmitmo.shared.client.TarotServiceClient
import com.github.butvinmitmo.shared.client.UserServiceClient
import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.InterpretationDto
import com.github.butvinmitmo.shared.dto.SpreadCardDto
import com.github.butvinmitmo.shared.dto.SpreadDto
import com.github.butvinmitmo.shared.dto.SpreadSummaryDto
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.UUID

@Component
class SpreadMapper(
    private val userServiceClient: UserServiceClient,
    private val tarotServiceClient: TarotServiceClient,
) {
    // System context for internal service-to-service calls
    private val systemUserId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private val systemRole = "SYSTEM"

    fun toDto(
        spread: Spread,
        spreadCards: List<SpreadCard>,
        interpretations: List<Interpretation>,
        cardCache: Map<UUID, CardDto> = emptyMap(),
    ): Mono<SpreadDto> {
        val authorMono =
            Mono
                .fromCallable { userServiceClient.getUserById(systemUserId, systemRole, spread.authorId).body!! }
                .subscribeOn(Schedulers.boundedElastic())

        val layoutTypeMono =
            Mono
                .fromCallable {
                    tarotServiceClient
                        .getLayoutTypeById(
                            systemUserId,
                            systemRole,
                            spread.layoutTypeId,
                        ).body!!
                }.subscribeOn(Schedulers.boundedElastic())

        val spreadCardsMono =
            Flux
                .fromIterable(spreadCards.sortedBy { it.positionInSpread })
                .flatMap { spreadCard ->
                    val cardMono =
                        if (cardCache.containsKey(spreadCard.cardId)) {
                            Mono.just(cardCache[spreadCard.cardId]!!)
                        } else {
                            fetchCardAsync(spreadCard.cardId)
                        }
                    cardMono.map { card ->
                        SpreadCardDto(
                            id = spreadCard.id!!,
                            card = card,
                            positionInSpread = spreadCard.positionInSpread,
                            isReversed = spreadCard.isReversed,
                        )
                    }
                }.collectList()

        val interpretationsMono =
            Flux
                .fromIterable(interpretations)
                .flatMap { interpretation ->
                    Mono
                        .fromCallable {
                            userServiceClient
                                .getUserById(
                                    systemUserId,
                                    systemRole,
                                    interpretation.authorId,
                                ).body!!
                        }.subscribeOn(Schedulers.boundedElastic())
                        .map { interpAuthor ->
                            InterpretationDto(
                                id = interpretation.id!!,
                                text = interpretation.text,
                                author = interpAuthor,
                                spreadId = interpretation.spreadId,
                                createdAt = interpretation.createdAt!!,
                            )
                        }
                }.collectList()

        return Mono
            .zip(authorMono, layoutTypeMono, spreadCardsMono, interpretationsMono)
            .map { tuple ->
                SpreadDto(
                    id = spread.id!!,
                    question = spread.question,
                    layoutType = tuple.t2,
                    cards = tuple.t3,
                    interpretations = tuple.t4,
                    author = tuple.t1,
                    createdAt = spread.createdAt!!,
                )
            }
    }

    fun toSummaryDto(
        spread: Spread,
        interpretationsCount: Int = 0,
    ): Mono<SpreadSummaryDto> {
        val authorMono =
            Mono
                .fromCallable { userServiceClient.getUserById(systemUserId, systemRole, spread.authorId).body!! }
                .subscribeOn(Schedulers.boundedElastic())

        val layoutTypeMono =
            Mono
                .fromCallable {
                    tarotServiceClient
                        .getLayoutTypeById(
                            systemUserId,
                            systemRole,
                            spread.layoutTypeId,
                        ).body!!
                }.subscribeOn(Schedulers.boundedElastic())

        return Mono.zip(authorMono, layoutTypeMono).map { tuple ->
            SpreadSummaryDto(
                id = spread.id!!,
                question = spread.question,
                layoutTypeName = tuple.t2.name,
                cardsCount = tuple.t2.cardsCount,
                interpretationsCount = interpretationsCount,
                authorUsername = tuple.t1.username,
                createdAt = spread.createdAt!!,
            )
        }
    }

    private fun fetchCardAsync(cardId: UUID): Mono<CardDto> =
        Mono
            .fromCallable {
                val allCards = mutableListOf<CardDto>()
                val pageSize = 50
                var page = 0
                var fetched: List<CardDto>
                do {
                    fetched = tarotServiceClient.getCards(systemUserId, systemRole, page, pageSize).body!!
                    allCards.addAll(fetched)
                    page++
                } while (fetched.size == pageSize)
                allCards.find { it.id == cardId }
                    ?: throw IllegalStateException("Could not fetch card with id $cardId")
            }.subscribeOn(Schedulers.boundedElastic())
}
