package com.github.butvinmitmo.divinationservice.service

import com.github.butvinmitmo.divinationservice.entity.Interpretation
import com.github.butvinmitmo.divinationservice.entity.Spread
import com.github.butvinmitmo.divinationservice.entity.SpreadCard
import com.github.butvinmitmo.divinationservice.exception.ConflictException
import com.github.butvinmitmo.divinationservice.exception.ForbiddenException
import com.github.butvinmitmo.divinationservice.exception.NotFoundException
import com.github.butvinmitmo.divinationservice.mapper.InterpretationMapper
import com.github.butvinmitmo.divinationservice.mapper.SpreadMapper
import com.github.butvinmitmo.divinationservice.repository.InterpretationRepository
import com.github.butvinmitmo.divinationservice.repository.SpreadCardRepository
import com.github.butvinmitmo.divinationservice.repository.SpreadRepository
import com.github.butvinmitmo.divinationservice.security.AuthorizationService
import com.github.butvinmitmo.shared.client.TarotServiceClient
import com.github.butvinmitmo.shared.client.UserServiceClient
import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.CreateInterpretationRequest
import com.github.butvinmitmo.shared.dto.CreateInterpretationResponse
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.CreateSpreadResponse
import com.github.butvinmitmo.shared.dto.InterpretationDto
import com.github.butvinmitmo.shared.dto.PageResponse
import com.github.butvinmitmo.shared.dto.ScrollResponse
import com.github.butvinmitmo.shared.dto.SpreadDto
import com.github.butvinmitmo.shared.dto.SpreadSummaryDto
import com.github.butvinmitmo.shared.dto.UpdateInterpretationRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.UUID
import kotlin.random.Random

@Service
class DivinationService(
    private val spreadRepository: SpreadRepository,
    private val spreadCardRepository: SpreadCardRepository,
    private val interpretationRepository: InterpretationRepository,
    private val userServiceClient: UserServiceClient,
    private val tarotServiceClient: TarotServiceClient,
    private val spreadMapper: SpreadMapper,
    private val interpretationMapper: InterpretationMapper,
    private val authorizationService: AuthorizationService,
) {
    @Transactional
    fun createSpread(request: CreateSpreadRequest): Mono<CreateSpreadResponse> =
        authorizationService.getCurrentUserId().flatMap { authorId ->
            authorizationService.getCurrentRole().flatMap { role ->
                // Validate user exists via Feign (blocking call on boundedElastic)
                Mono
                    .fromCallable { userServiceClient.getUserById(authorId, role, authorId) }
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap {
                        // Validate layout type exists via Feign (blocking call on boundedElastic)
                        Mono
                            .fromCallable {
                                tarotServiceClient
                                    .getLayoutTypeById(
                                        authorId,
                                        role,
                                        request.layoutTypeId,
                                    ).body!!
                            }.subscribeOn(Schedulers.boundedElastic())
                    }.flatMap { layoutType ->
                        val spread =
                            Spread(
                                question = request.question,
                                authorId = authorId,
                                layoutTypeId = request.layoutTypeId,
                            )
                        spreadRepository
                            .save(spread)
                            .flatMap { savedSpread ->
                                // Get random cards via Feign
                                Mono
                                    .fromCallable {
                                        tarotServiceClient
                                            .getRandomCards(
                                                authorId,
                                                role,
                                                layoutType.cardsCount,
                                            ).body!!
                                    }.subscribeOn(Schedulers.boundedElastic())
                                    .flatMapMany { cards ->
                                        // Save all spread cards reactively
                                        Flux.fromIterable(
                                            cards.mapIndexed { index, card ->
                                                SpreadCard(
                                                    spreadId = savedSpread.id!!,
                                                    cardId = card.id,
                                                    positionInSpread = index + 1,
                                                    isReversed = Random.nextBoolean(),
                                                )
                                            },
                                        )
                                    }.flatMap { spreadCard ->
                                        spreadCardRepository.save(spreadCard)
                                    }.then(Mono.just(CreateSpreadResponse(id = savedSpread.id!!)))
                            }
                    }
            }
        }

    fun getSpreads(
        page: Int,
        size: Int,
    ): Mono<PageResponse<SpreadSummaryDto>> {
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
                                val totalPages = (totalElements + size - 1) / size
                                Flux
                                    .fromIterable(spreads)
                                    .flatMap { spread ->
                                        spreadMapper.toSummaryDto(spread, interpretationCounts[spread.id] ?: 0)
                                    }.collectList()
                                    .map { summaries ->
                                        PageResponse(
                                            content = summaries,
                                            page = page,
                                            size = size,
                                            totalElements = totalElements,
                                            totalPages = totalPages.toInt(),
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
    ): Mono<ScrollResponse<SpreadSummaryDto>> {
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
                        Flux
                            .fromIterable(itemsToReturn)
                            .flatMap { spread ->
                                spreadMapper.toSummaryDto(spread, interpretationCounts[spread.id] ?: 0)
                            }.collectList()
                            .map { summaries ->
                                ScrollResponse(
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

    @Transactional
    fun getSpread(id: UUID): Mono<SpreadDto> =
        spreadRepository
            .findByIdWithCards(id)
            .switchIfEmpty(Mono.error(NotFoundException("Spread not found")))
            .flatMap { spread ->
                // Load spread cards separately (R2DBC doesn't support @OneToMany)
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

                        // Build card cache
                        val cardIds = spreadCards.map { it.cardId }.toSet()
                        buildCardCache(cardIds)
                            .flatMap { cardCache ->
                                spreadMapper.toDto(spread, spreadCards, interpretations, cardCache)
                            }
                    }
            }

    @Transactional
    fun deleteSpread(id: UUID): Mono<Void> =
        spreadRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Spread not found")))
            .flatMap { spread ->
                authorizationService.canModify(spread.authorId).flatMap { canModify ->
                    if (!canModify) {
                        Mono.error(ForbiddenException("You can only delete your own spreads"))
                    } else {
                        spreadRepository.deleteById(id)
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
        request: CreateInterpretationRequest,
    ): Mono<CreateInterpretationResponse> =
        authorizationService.getCurrentUserId().flatMap { authorId ->
            authorizationService.getCurrentRole().flatMap { role ->
                getSpreadEntity(spreadId)
                    .flatMap {
                        // Validate user exists via Feign (blocking call on boundedElastic)
                        Mono
                            .fromCallable { userServiceClient.getUserById(authorId, role, authorId) }
                            .subscribeOn(Schedulers.boundedElastic())
                    }.flatMap {
                        interpretationRepository.existsByAuthorAndSpread(authorId, spreadId)
                    }.flatMap { exists ->
                        if (exists) {
                            Mono.error(ConflictException("You already have an interpretation for this spread"))
                        } else {
                            val interpretation =
                                Interpretation(
                                    text = request.text,
                                    authorId = authorId,
                                    spreadId = spreadId,
                                )
                            interpretationRepository
                                .save(interpretation)
                                .map { saved -> CreateInterpretationResponse(id = saved.id!!) }
                        }
                    }
            }
        }

    @Transactional
    fun updateInterpretation(
        spreadId: UUID,
        id: UUID,
        request: UpdateInterpretationRequest,
    ): Mono<InterpretationDto> =
        interpretationRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Interpretation not found")))
            .flatMap { interpretation ->
                authorizationService.canModify(interpretation.authorId).flatMap { canModify ->
                    if (!canModify) {
                        Mono.error(ForbiddenException("You can only edit your own interpretations"))
                    } else {
                        interpretation.text = request.text
                        interpretationRepository
                            .save(interpretation)
                            .flatMap { saved -> interpretationMapper.toDto(saved) }
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
                authorizationService.canModify(interpretation.authorId).flatMap { canModify ->
                    if (!canModify) {
                        Mono.error(ForbiddenException("You can only delete your own interpretations"))
                    } else {
                        interpretationRepository.deleteById(id)
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
                    interpretationMapper.toDto(interpretation)
                }
            }

    @Transactional(readOnly = true)
    fun getInterpretations(
        spreadId: UUID,
        page: Int,
        size: Int,
    ): Mono<PageResponse<InterpretationDto>> =
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
                        val totalPages = (totalElements + size - 1) / size

                        Flux
                            .fromIterable(interpretations)
                            .flatMap { interpretationMapper.toDto(it) }
                            .collectList()
                            .map { dtos ->
                                PageResponse(
                                    content = dtos,
                                    page = page,
                                    size = size,
                                    totalElements = totalElements,
                                    totalPages = totalPages.toInt(),
                                    isFirst = page == 0,
                                    isLast = page >= totalPages - 1,
                                )
                            }
                    }
            }

    private fun buildCardCache(cardIds: Set<UUID>): Mono<Map<UUID, CardDto>> {
        if (cardIds.isEmpty()) return Mono.just(emptyMap())
        val systemUserId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        return Mono
            .fromCallable {
                val allCards = mutableListOf<CardDto>()
                val pageSize = 50
                var page = 0
                var fetched: List<CardDto>
                do {
                    fetched = tarotServiceClient.getCards(systemUserId, "SYSTEM", page, pageSize).body!!
                    allCards.addAll(fetched)
                    page++
                } while (fetched.size == pageSize)
                allCards.filter { it.id in cardIds }.associateBy { it.id }
            }.subscribeOn(Schedulers.boundedElastic())
    }

    /**
     * Deletes all data associated with a user (spreads and interpretations).
     * Called by user-service before deleting a user to ensure cascade cleanup.
     * Spread cards are automatically deleted via database cascade (internal FK).
     *
     * @param userId The ID of the user whose data should be deleted
     * @return Mono<Void> that completes when deletion is done
     */
    @Transactional
    fun deleteUserData(userId: UUID): Mono<Void> =
        // First delete all interpretations by this user
        interpretationRepository
            .deleteByAuthorId(userId)
            // Then delete all spreads by this user (spread_cards cascade via internal FK)
            .then(spreadRepository.deleteByAuthorId(userId))
            .then()
}
