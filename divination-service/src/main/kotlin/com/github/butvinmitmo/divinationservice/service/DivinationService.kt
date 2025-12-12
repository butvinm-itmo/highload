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
) {
    @Transactional
    fun createSpread(request: CreateSpreadRequest): Mono<CreateSpreadResponse> {
        // Validate user exists via Feign (blocking call on boundedElastic)
        return Mono
            .fromCallable { userServiceClient.getUserById(request.authorId) }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap {
                // Validate layout type exists via Feign (blocking call on boundedElastic)
                Mono
                    .fromCallable { tarotServiceClient.getLayoutTypeById(request.layoutTypeId).body!! }
                    .subscribeOn(Schedulers.boundedElastic())
            }.flatMap { layoutType ->
                val spread =
                    Spread(
                        question = request.question,
                        authorId = request.authorId,
                        layoutTypeId = request.layoutTypeId,
                    )
                spreadRepository
                    .save(spread)
                    .flatMap { savedSpread ->
                        // Get random cards via Feign
                        Mono
                            .fromCallable { tarotServiceClient.getRandomCards(layoutType.cardsCount).body!! }
                            .subscribeOn(Schedulers.boundedElastic())
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
                            .map { interpretationCounts ->
                                val totalPages = (totalElements + size - 1) / size
                                PageResponse(
                                    content =
                                        spreads.map { spread ->
                                            spreadMapper.toSummaryDto(spread, interpretationCounts[spread.id] ?: 0)
                                        },
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
                    .map { interpretationCounts ->
                        ScrollResponse(
                            items =
                                itemsToReturn.map { spread ->
                                    spreadMapper.toSummaryDto(spread, interpretationCounts[spread.id] ?: 0)
                                },
                            nextCursor = nextCursor,
                        )
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
                            .map { cardCache ->
                                spreadMapper.toDto(spread, spreadCards, interpretations, cardCache)
                            }
                    }
            }

    @Transactional
    fun deleteSpread(
        id: UUID,
        userId: UUID,
    ): Mono<Void> =
        spreadRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Spread not found")))
            .flatMap { spread ->
                if (spread.authorId != userId) {
                    Mono.error(ForbiddenException("You can only delete your own spreads"))
                } else {
                    spreadRepository.deleteById(id)
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
        getSpreadEntity(spreadId)
            .flatMap {
                // Validate user exists via Feign (blocking call on boundedElastic)
                Mono
                    .fromCallable { userServiceClient.getUserById(request.authorId) }
                    .subscribeOn(Schedulers.boundedElastic())
            }.flatMap {
                interpretationRepository.existsByAuthorAndSpread(request.authorId, spreadId)
            }.flatMap { exists ->
                if (exists) {
                    Mono.error(ConflictException("You already have an interpretation for this spread"))
                } else {
                    val interpretation =
                        Interpretation(
                            text = request.text,
                            authorId = request.authorId,
                            spreadId = spreadId,
                        )
                    interpretationRepository
                        .save(interpretation)
                        .map { saved -> CreateInterpretationResponse(id = saved.id!!) }
                }
            }

    @Transactional
    fun updateInterpretation(
        spreadId: UUID,
        id: UUID,
        userId: UUID,
        request: UpdateInterpretationRequest,
    ): Mono<InterpretationDto> =
        interpretationRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Interpretation not found")))
            .flatMap { interpretation ->
                if (interpretation.authorId != userId) {
                    Mono.error(ForbiddenException("You can only edit your own interpretations"))
                } else {
                    interpretation.text = request.text
                    interpretationRepository
                        .save(interpretation)
                        .map { saved -> interpretationMapper.toDto(saved) }
                }
            }

    @Transactional
    fun deleteInterpretation(
        spreadId: UUID,
        id: UUID,
        userId: UUID,
    ): Mono<Void> =
        interpretationRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Interpretation not found")))
            .flatMap { interpretation ->
                if (interpretation.authorId != userId) {
                    Mono.error(ForbiddenException("You can only delete your own interpretations"))
                } else {
                    interpretationRepository.deleteById(id)
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
                    Mono.just(interpretationMapper.toDto(interpretation))
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
                val interpretationsFlux = interpretationRepository.findBySpreadIdOrderByCreatedAtDesc(spreadId, offset, size)
                val countMono = interpretationRepository.countBySpreadId(spreadId)

                Mono.zip(interpretationsFlux.collectList(), countMono)
                    .map { tuple ->
                        val interpretations = tuple.t1
                        val totalElements = tuple.t2
                        val totalPages = (totalElements + size - 1) / size

                        PageResponse(
                            content = interpretations.map { interpretationMapper.toDto(it) },
                            page = page,
                            size = size,
                            totalElements = totalElements,
                            totalPages = totalPages.toInt(),
                            isFirst = page == 0,
                            isLast = page >= totalPages - 1,
                        )
                    }
            }

    private fun buildCardCache(cardIds: Set<UUID>): Mono<Map<UUID, CardDto>> {
        // For now, we fetch random cards for each position
        // In production, you might want to add a batch endpoint to fetch cards by IDs
        return Mono
            .fromCallable {
                val cards = tarotServiceClient.getRandomCards(cardIds.size).body!!
                cardIds.zip(cards).toMap()
            }.subscribeOn(Schedulers.boundedElastic())
    }
}
