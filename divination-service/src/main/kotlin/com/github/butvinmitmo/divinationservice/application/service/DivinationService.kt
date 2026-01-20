package com.github.butvinmitmo.divinationservice.application.service

import com.github.butvinmitmo.divinationservice.application.interfaces.provider.CardProvider
import com.github.butvinmitmo.divinationservice.application.interfaces.provider.CurrentUserProvider
import com.github.butvinmitmo.divinationservice.application.interfaces.provider.UserProvider
import com.github.butvinmitmo.divinationservice.application.interfaces.publisher.InterpretationEventPublisher
import com.github.butvinmitmo.divinationservice.application.interfaces.publisher.SpreadEventPublisher
import com.github.butvinmitmo.divinationservice.application.interfaces.repository.InterpretationRepository
import com.github.butvinmitmo.divinationservice.application.interfaces.repository.SpreadCardRepository
import com.github.butvinmitmo.divinationservice.application.interfaces.repository.SpreadRepository
import com.github.butvinmitmo.divinationservice.domain.model.Interpretation
import com.github.butvinmitmo.divinationservice.domain.model.Spread
import com.github.butvinmitmo.divinationservice.domain.model.SpreadCard
import com.github.butvinmitmo.divinationservice.exception.ConflictException
import com.github.butvinmitmo.divinationservice.exception.ForbiddenException
import com.github.butvinmitmo.divinationservice.exception.NotFoundException
import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.InterpretationDto
import com.github.butvinmitmo.shared.dto.SpreadCardDto
import com.github.butvinmitmo.shared.dto.SpreadDto
import com.github.butvinmitmo.shared.dto.SpreadSummaryDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID
import kotlin.random.Random

data class PageResult<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
    val isFirst: Boolean,
    val isLast: Boolean,
)

data class ScrollResult<T>(
    val items: List<T>,
    val nextCursor: UUID?,
)

data class CreateSpreadResult(
    val id: UUID,
)

data class CreateInterpretationResult(
    val id: UUID,
)

@Service
class DivinationService(
    private val spreadRepository: SpreadRepository,
    private val spreadCardRepository: SpreadCardRepository,
    private val interpretationRepository: InterpretationRepository,
    private val userProvider: UserProvider,
    private val cardProvider: CardProvider,
    private val currentUserProvider: CurrentUserProvider,
    private val spreadEventPublisher: SpreadEventPublisher,
    private val interpretationEventPublisher: InterpretationEventPublisher,
) {
    @Transactional
    fun createSpread(
        question: String?,
        layoutTypeId: UUID,
    ): Mono<CreateSpreadResult> =
        currentUserProvider.getCurrentUserId().flatMap { authorId ->
            currentUserProvider.getCurrentRole().flatMap { role ->
                // Validate user exists
                userProvider
                    .getUserById(authorId, role, authorId)
                    .flatMap {
                        // Validate layout type exists and get cards count
                        cardProvider.getLayoutTypeById(authorId, role, layoutTypeId)
                    }.flatMap { layoutType ->
                        val spread =
                            Spread(
                                id = null,
                                question = question,
                                authorId = authorId,
                                layoutTypeId = layoutTypeId,
                                createdAt = null,
                            )
                        spreadRepository
                            .save(spread)
                            .flatMap { savedSpread ->
                                // Get random cards
                                cardProvider
                                    .getRandomCards(authorId, role, layoutType.cardsCount)
                                    .flatMapMany { cards ->
                                        // Save all spread cards
                                        Flux.fromIterable(
                                            cards.mapIndexed { index, card ->
                                                SpreadCard(
                                                    id = null,
                                                    spreadId = savedSpread.id!!,
                                                    cardId = card.id,
                                                    positionInSpread = index + 1,
                                                    isReversed = Random.nextBoolean(),
                                                )
                                            },
                                        )
                                    }.flatMap { spreadCard ->
                                        spreadCardRepository.save(spreadCard)
                                    }.then(spreadEventPublisher.publishCreated(savedSpread))
                                    .then(Mono.just(CreateSpreadResult(id = savedSpread.id!!)))
                            }
                    }
            }
        }

    fun getSpreads(
        page: Int,
        size: Int,
    ): Mono<PageResult<SpreadSummaryDto>> {
        val offset = page.toLong() * size
        return spreadRepository
            .count()
            .flatMap { totalElements ->
                spreadRepository
                    .findAllOrderByCreatedAtDesc(offset, size)
                    .collectList()
                    .flatMap { spreads ->
                        val spreadIds = spreads.map { it.id!! }
                        getInterpretationCounts(spreadIds)
                            .flatMap { interpretationCounts ->
                                val totalPages = ((totalElements + size - 1) / size).toInt()
                                buildSpreadSummaries(spreads, interpretationCounts)
                                    .map { summaries ->
                                        PageResult(
                                            content = summaries,
                                            page = page,
                                            size = size,
                                            totalElements = totalElements,
                                            totalPages = totalPages,
                                            isFirst = page == 0,
                                            isLast = page >= totalPages - 1,
                                        )
                                    }
                            }
                    }
            }
    }

    @Transactional(readOnly = true)
    fun getSpreadsByScroll(
        after: UUID?,
        size: Int,
    ): Mono<ScrollResult<SpreadSummaryDto>> {
        val spreadsFlux =
            if (after != null) {
                spreadRepository.findSpreadsAfterCursor(after, size + 1)
            } else {
                spreadRepository.findLatestSpreads(size + 1)
            }

        return spreadsFlux
            .collectList()
            .flatMap { spreads ->
                val hasMore = spreads.size > size
                val itemsToReturn = if (hasMore) spreads.take(size) else spreads
                val nextCursor = if (hasMore) itemsToReturn.last().id else null

                val spreadIds = itemsToReturn.map { it.id!! }
                getInterpretationCounts(spreadIds)
                    .flatMap { interpretationCounts ->
                        buildSpreadSummaries(itemsToReturn, interpretationCounts)
                            .map { summaries ->
                                ScrollResult(
                                    items = summaries,
                                    nextCursor = nextCursor,
                                )
                            }
                    }
            }
    }

    private fun getInterpretationCounts(spreadIds: List<UUID>): Mono<Map<UUID, Int>> {
        if (spreadIds.isEmpty()) return Mono.just(emptyMap())
        return interpretationRepository
            .countBySpreadIds(spreadIds)
            .collectMap({ it.first }, { it.second.toInt() })
    }

    private fun buildSpreadSummaries(
        spreads: List<Spread>,
        interpretationCounts: Map<UUID, Int>,
    ): Mono<List<SpreadSummaryDto>> =
        Flux
            .fromIterable(spreads)
            .flatMap { spread ->
                Mono
                    .zip(
                        userProvider.getSystemUser(spread.authorId),
                        cardProvider.getSystemLayoutType(spread.layoutTypeId),
                    ).map { tuple ->
                        val author = tuple.t1
                        val layoutType = tuple.t2
                        SpreadSummaryDto(
                            id = spread.id!!,
                            question = spread.question,
                            layoutTypeName = layoutType.name,
                            cardsCount = layoutType.cardsCount,
                            interpretationsCount = interpretationCounts[spread.id] ?: 0,
                            authorUsername = author.username,
                            createdAt = spread.createdAt!!,
                        )
                    }
            }.collectList()

    @Transactional
    fun getSpread(id: UUID): Mono<SpreadDto> =
        spreadRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Spread not found")))
            .flatMap { spread ->
                // Load spread cards
                val spreadCardsFlux = spreadCardRepository.findBySpreadId(spread.id!!)
                // Load interpretations (limit 50)
                val interpretationsFlux =
                    interpretationRepository
                        .findBySpreadIdOrderByCreatedAtDesc(spread.id!!, 0, 50)

                Mono
                    .zip(spreadCardsFlux.collectList(), interpretationsFlux.collectList())
                    .flatMap { tuple ->
                        val spreadCards = tuple.t1
                        val interpretations = tuple.t2

                        // Get all cards for this spread
                        val cardIds = spreadCards.map { it.cardId }.toSet()
                        buildCardCache(cardIds)
                            .flatMap { cardCache ->
                                buildSpreadDto(spread, spreadCards, interpretations, cardCache)
                            }
                    }
            }

    private fun buildCardCache(cardIds: Set<UUID>): Mono<Map<UUID, CardDto>> {
        if (cardIds.isEmpty()) return Mono.just(emptyMap())
        return cardProvider
            .getAllCards()
            .map { allCards -> allCards.filter { it.id in cardIds }.associateBy { it.id } }
    }

    private fun buildSpreadDto(
        spread: Spread,
        spreadCards: List<SpreadCard>,
        interpretations: List<Interpretation>,
        cardCache: Map<UUID, CardDto>,
    ): Mono<SpreadDto> {
        val authorMono = userProvider.getSystemUser(spread.authorId)
        val layoutTypeMono = cardProvider.getSystemLayoutType(spread.layoutTypeId)

        val spreadCardsMono =
            Flux
                .fromIterable(spreadCards.sortedBy { it.positionInSpread })
                .flatMap { spreadCard ->
                    val cardMono =
                        if (cardCache.containsKey(spreadCard.cardId)) {
                            Mono.just(cardCache[spreadCard.cardId]!!)
                        } else {
                            fetchCardById(spreadCard.cardId)
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
                    userProvider
                        .getSystemUser(interpretation.authorId)
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

    private fun fetchCardById(cardId: UUID): Mono<CardDto> =
        cardProvider
            .getAllCards()
            .map { allCards ->
                allCards.find { it.id == cardId }
                    ?: throw IllegalStateException("Could not find card with id $cardId")
            }

    @Transactional
    fun deleteSpread(id: UUID): Mono<Void> =
        spreadRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Spread not found")))
            .flatMap { spread ->
                currentUserProvider.canModify(spread.authorId).flatMap { canModify ->
                    if (!canModify) {
                        Mono.error(ForbiddenException("You can only delete your own spreads"))
                    } else {
                        spreadRepository
                            .deleteById(id)
                            .then(spreadEventPublisher.publishDeleted(spread))
                    }
                }
            }

    fun getSpreadEntity(id: UUID): Mono<Spread> =
        spreadRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Spread not found")))

    @Transactional
    fun addInterpretation(
        spreadId: UUID,
        text: String,
    ): Mono<CreateInterpretationResult> =
        currentUserProvider.getCurrentUserId().flatMap { authorId ->
            currentUserProvider.getCurrentRole().flatMap { role ->
                getSpreadEntity(spreadId)
                    .flatMap {
                        // Validate user exists
                        userProvider.getUserById(authorId, role, authorId)
                    }.flatMap {
                        interpretationRepository.existsByAuthorAndSpread(authorId, spreadId)
                    }.flatMap { exists ->
                        if (exists) {
                            Mono.error(ConflictException("You already have an interpretation for this spread"))
                        } else {
                            val interpretation =
                                Interpretation(
                                    id = null,
                                    text = text,
                                    authorId = authorId,
                                    spreadId = spreadId,
                                    createdAt = null,
                                )
                            interpretationRepository
                                .save(interpretation)
                                .flatMap { saved ->
                                    interpretationEventPublisher
                                        .publishCreated(saved)
                                        .then(Mono.just(CreateInterpretationResult(id = saved.id!!)))
                                }
                        }
                    }
            }
        }

    @Transactional
    fun updateInterpretation(
        spreadId: UUID,
        id: UUID,
        newText: String,
    ): Mono<InterpretationDto> =
        interpretationRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Interpretation not found")))
            .flatMap { interpretation ->
                currentUserProvider.canModify(interpretation.authorId).flatMap { canModify ->
                    if (!canModify) {
                        Mono.error(ForbiddenException("You can only edit your own interpretations"))
                    } else {
                        val updated = interpretation.copy(text = newText)
                        interpretationRepository
                            .save(updated)
                            .flatMap { saved ->
                                interpretationEventPublisher
                                    .publishUpdated(saved)
                                    .then(
                                        userProvider
                                            .getSystemUser(saved.authorId)
                                            .map { author ->
                                                InterpretationDto(
                                                    id = saved.id!!,
                                                    text = saved.text,
                                                    author = author,
                                                    spreadId = saved.spreadId,
                                                    createdAt = saved.createdAt!!,
                                                )
                                            },
                                    )
                            }
                    }
                }
            }

    @Transactional
    fun deleteInterpretation(
        spreadId: UUID,
        id: UUID,
    ): Mono<Void> =
        interpretationRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Interpretation not found")))
            .flatMap { interpretation ->
                currentUserProvider.canModify(interpretation.authorId).flatMap { canModify ->
                    if (!canModify) {
                        Mono.error(ForbiddenException("You can only delete your own interpretations"))
                    } else {
                        interpretationRepository
                            .deleteById(id)
                            .then(interpretationEventPublisher.publishDeleted(interpretation))
                    }
                }
            }

    @Transactional(readOnly = true)
    fun getInterpretation(
        spreadId: UUID,
        id: UUID,
    ): Mono<InterpretationDto> =
        interpretationRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Interpretation not found")))
            .flatMap { interpretation ->
                if (interpretation.spreadId != spreadId) {
                    Mono.error(NotFoundException("Interpretation not found in this spread"))
                } else {
                    userProvider
                        .getSystemUser(interpretation.authorId)
                        .map { author ->
                            InterpretationDto(
                                id = interpretation.id!!,
                                text = interpretation.text,
                                author = author,
                                spreadId = interpretation.spreadId,
                                createdAt = interpretation.createdAt!!,
                            )
                        }
                }
            }

    @Transactional(readOnly = true)
    fun getInterpretations(
        spreadId: UUID,
        page: Int,
        size: Int,
    ): Mono<PageResult<InterpretationDto>> =
        getSpreadEntity(spreadId)
            .flatMap {
                val offset = page.toLong() * size
                val interpretationsFlux =
                    interpretationRepository.findBySpreadIdOrderByCreatedAtDesc(
                        spreadId,
                        offset,
                        size,
                    )
                val countMono = interpretationRepository.countBySpreadId(spreadId)

                Mono
                    .zip(interpretationsFlux.collectList(), countMono)
                    .flatMap { tuple ->
                        val interpretations = tuple.t1
                        val totalElements = tuple.t2
                        val totalPages = ((totalElements + size - 1) / size).toInt()

                        Flux
                            .fromIterable(interpretations)
                            .flatMap { interpretation ->
                                userProvider
                                    .getSystemUser(interpretation.authorId)
                                    .map { author ->
                                        InterpretationDto(
                                            id = interpretation.id!!,
                                            text = interpretation.text,
                                            author = author,
                                            spreadId = interpretation.spreadId,
                                            createdAt = interpretation.createdAt!!,
                                        )
                                    }
                            }.collectList()
                            .map { dtos ->
                                PageResult(
                                    content = dtos,
                                    page = page,
                                    size = size,
                                    totalElements = totalElements,
                                    totalPages = totalPages,
                                    isFirst = page == 0,
                                    isLast = page >= totalPages - 1,
                                )
                            }
                    }
            }

    @Transactional
    fun deleteUserData(userId: UUID): Mono<Void> =
        // First delete all interpretations by this user
        interpretationRepository
            .deleteByAuthorId(userId)
            // Then delete all spreads by this user (spread_cards cascade via internal FK)
            .then(spreadRepository.deleteByAuthorId(userId))
            .then()
}
