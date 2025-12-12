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
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    fun createSpread(request: CreateSpreadRequest): CreateSpreadResponse {
        // Validate user exists via Feign (throws FeignException.NotFound if not found)
        userServiceClient.getUserById(request.authorId)
        // Validate layout type exists via Feign (throws FeignException.NotFound if not found)
        val layoutType = tarotServiceClient.getLayoutTypeById(request.layoutTypeId).body!!

        val spread =
            Spread(
                question = request.question,
                authorId = request.authorId,
                layoutTypeId = request.layoutTypeId,
            )
        val savedSpread = spreadRepository.save(spread)

        val cards = tarotServiceClient.getRandomCards(layoutType.cardsCount).body!!

        cards.forEachIndexed { index, card ->
            val spreadCard =
                SpreadCard(
                    spread = savedSpread,
                    cardId = card.id,
                    positionInSpread = index + 1,
                    isReversed = Random.nextBoolean(),
                )
            spreadCardRepository.save(spreadCard)
        }

        return CreateSpreadResponse(id = savedSpread.id)
    }

    fun getSpreads(
        page: Int,
        size: Int,
    ): PageResponse<SpreadSummaryDto> {
        val pageable = PageRequest.of(page, size)
        val spreadsPage = spreadRepository.findAllOrderByCreatedAtDesc(pageable)

        val spreadIds = spreadsPage.content.map { it.id }
        val interpretationCounts = getInterpretationCounts(spreadIds)

        return PageResponse(
            content =
                spreadsPage.content.map { spread ->
                    spreadMapper.toSummaryDto(spread, interpretationCounts[spread.id] ?: 0)
                },
            page = spreadsPage.number,
            size = spreadsPage.size,
            totalElements = spreadsPage.totalElements,
            totalPages = spreadsPage.totalPages,
            isFirst = spreadsPage.isFirst,
            isLast = spreadsPage.isLast,
        )
    }

    @Transactional(readOnly = true)
    fun getSpreadsByScroll(
        after: UUID?,
        size: Int,
    ): ScrollResponse<SpreadSummaryDto> {
        val spreads =
            if (after != null) {
                spreadRepository.findSpreadsAfterCursor(after, size + 1)
            } else {
                spreadRepository.findLatestSpreads(size + 1)
            }

        val hasMore = spreads.size > size
        val itemsToReturn = if (hasMore) spreads.take(size) else spreads
        val nextCursor = if (hasMore) itemsToReturn.last().id else null

        val spreadIds = itemsToReturn.map { it.id }
        val interpretationCounts = getInterpretationCounts(spreadIds)

        return ScrollResponse(
            items =
                itemsToReturn.map { spread ->
                    spreadMapper.toSummaryDto(spread, interpretationCounts[spread.id] ?: 0)
                },
            nextCursor = nextCursor,
        )
    }

    private fun getInterpretationCounts(spreadIds: List<UUID>): Map<UUID, Int> {
        if (spreadIds.isEmpty()) return emptyMap()
        val results = interpretationRepository.countBySpreadIds(spreadIds)
        return results.associate { row ->
            val spreadId = row[0] as UUID
            val count = (row[1] as Long).toInt()
            spreadId to count
        }
    }

    @Transactional
    fun getSpread(id: UUID): SpreadDto {
        val spread =
            spreadRepository.findByIdWithCards(id)
                ?: throw NotFoundException("Spread not found")

        val interpretations =
            interpretationRepository
                .findBySpreadIdOrderByCreatedAtDesc(
                    spread.id,
                    PageRequest.of(0, 50),
                ).content

        // Build a cache of cards for this spread
        val cardIds = spread.spreadCards.map { it.cardId }.toSet()
        val cardCache = buildCardCache(cardIds)

        return spreadMapper.toDto(spread, interpretations, cardCache)
    }

    @Transactional
    fun deleteSpread(
        id: UUID,
        userId: UUID,
    ) {
        val spread =
            spreadRepository
                .findById(id)
                .orElseThrow { NotFoundException("Spread not found") }

        if (spread.authorId != userId) {
            throw ForbiddenException("You can only delete your own spreads")
        }

        spreadRepository.deleteById(id)
    }

    fun getSpreadEntity(id: UUID): Spread =
        spreadRepository
            .findById(id)
            .orElseThrow { NotFoundException("Spread not found") }

    @Transactional
    fun addInterpretation(
        spreadId: UUID,
        request: CreateInterpretationRequest,
    ): CreateInterpretationResponse {
        val spread = getSpreadEntity(spreadId)
        // Validate user exists via Feign (throws FeignException.NotFound if not found)
        userServiceClient.getUserById(request.authorId)

        if (interpretationRepository.existsByAuthorAndSpread(request.authorId, spreadId)) {
            throw ConflictException("You already have an interpretation for this spread")
        }

        val interpretation =
            Interpretation(
                text = request.text,
                authorId = request.authorId,
                spread = spread,
            )

        val saved = interpretationRepository.save(interpretation)
        return CreateInterpretationResponse(id = saved.id)
    }

    @Transactional
    fun updateInterpretation(
        spreadId: UUID,
        id: UUID,
        userId: UUID,
        request: UpdateInterpretationRequest,
    ): InterpretationDto {
        val interpretation =
            interpretationRepository
                .findById(id)
                .orElseThrow { NotFoundException("Interpretation not found") }

        if (interpretation.authorId != userId) {
            throw ForbiddenException("You can only edit your own interpretations")
        }

        interpretation.text = request.text
        val saved = interpretationRepository.save(interpretation)

        return interpretationMapper.toDto(saved)
    }

    @Transactional
    fun deleteInterpretation(
        spreadId: UUID,
        id: UUID,
        userId: UUID,
    ) {
        val interpretation =
            interpretationRepository
                .findById(id)
                .orElseThrow { NotFoundException("Interpretation not found") }

        if (interpretation.authorId != userId) {
            throw ForbiddenException("You can only delete your own interpretations")
        }

        interpretationRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun getInterpretation(
        spreadId: UUID,
        id: UUID,
    ): InterpretationDto {
        val interpretation =
            interpretationRepository
                .findById(id)
                .orElseThrow { NotFoundException("Interpretation not found") }

        if (interpretation.spread.id != spreadId) {
            throw NotFoundException("Interpretation not found in this spread")
        }

        return interpretationMapper.toDto(interpretation)
    }

    @Transactional(readOnly = true)
    fun getInterpretations(
        spreadId: UUID,
        page: Int,
        size: Int,
    ): PageResponse<InterpretationDto> {
        getSpreadEntity(spreadId)
        val pageable = PageRequest.of(page, size)
        val interpretationsPage = interpretationRepository.findBySpreadIdOrderByCreatedAtDesc(spreadId, pageable)
        return PageResponse(
            content = interpretationsPage.content.map { interpretationMapper.toDto(it) },
            page = interpretationsPage.number,
            size = interpretationsPage.size,
            totalElements = interpretationsPage.totalElements,
            totalPages = interpretationsPage.totalPages,
            isFirst = interpretationsPage.isFirst,
            isLast = interpretationsPage.isLast,
        )
    }

    private fun buildCardCache(cardIds: Set<UUID>): Map<UUID, CardDto> {
        // For now, we fetch random cards for each position
        // In production, you might want to add a batch endpoint to fetch cards by IDs
        val cards = tarotServiceClient.getRandomCards(cardIds.size).body!!
        return cardIds.zip(cards).toMap()
    }
}
