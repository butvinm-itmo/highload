package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.dto.CreateInterpretationRequest
import com.github.butvinmitmo.highload.dto.CreateInterpretationResponse
import com.github.butvinmitmo.highload.dto.CreateSpreadRequest
import com.github.butvinmitmo.highload.dto.CreateSpreadResponse
import com.github.butvinmitmo.highload.dto.InterpretationDto
import com.github.butvinmitmo.highload.dto.PageResponse
import com.github.butvinmitmo.highload.dto.ScrollResponse
import com.github.butvinmitmo.highload.dto.SpreadDto
import com.github.butvinmitmo.highload.dto.SpreadSummaryDto
import com.github.butvinmitmo.highload.dto.UpdateInterpretationRequest
import com.github.butvinmitmo.highload.entity.Interpretation
import com.github.butvinmitmo.highload.entity.Spread
import com.github.butvinmitmo.highload.entity.SpreadCard
import com.github.butvinmitmo.highload.exception.ConflictException
import com.github.butvinmitmo.highload.exception.ForbiddenException
import com.github.butvinmitmo.highload.exception.NotFoundException
import com.github.butvinmitmo.highload.mapper.InterpretationMapper
import com.github.butvinmitmo.highload.mapper.SpreadMapper
import com.github.butvinmitmo.highload.repository.InterpretationRepository
import com.github.butvinmitmo.highload.repository.SpreadCardRepository
import com.github.butvinmitmo.highload.repository.SpreadRepository
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
    private val userService: UserService,
    private val tarotService: TarotService,
    private val spreadMapper: SpreadMapper,
    private val interpretationMapper: InterpretationMapper,
) {
    // ==================== Spread Operations ====================

    @Transactional
    fun createSpread(request: CreateSpreadRequest): CreateSpreadResponse {
        val user = userService.getUserEntity(request.authorId)
        val layoutType = tarotService.getLayoutTypeById(request.layoutTypeId)

        val spread =
            Spread(
                question = request.question,
                author = user,
                layoutType = layoutType,
            )
        val savedSpread = spreadRepository.save(spread)

        val cards = tarotService.getRandomCards(layoutType.cardsCount)

        cards.forEachIndexed { index, card ->
            val spreadCard =
                SpreadCard(
                    spread = savedSpread,
                    card = card,
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

        return PageResponse(
            content = spreadsPage.content.map { spreadMapper.toSummaryDto(it) },
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

        return ScrollResponse(
            items = itemsToReturn.map { spreadMapper.toSummaryDto(it) },
            nextCursor = nextCursor,
        )
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

        return spreadMapper.toDto(spread, interpretations)
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

        if (spread.author.id != userId) {
            throw ForbiddenException("You can only delete your own spreads")
        }

        spreadRepository.deleteById(id)
    }

    fun getSpreadEntity(id: UUID): Spread =
        spreadRepository
            .findById(id)
            .orElseThrow { NotFoundException("Spread not found") }

    // ==================== Interpretation Operations ====================

    @Transactional
    fun addInterpretation(
        spreadId: UUID,
        request: CreateInterpretationRequest,
    ): CreateInterpretationResponse {
        val spread = getSpreadEntity(spreadId)
        val user = userService.getUserEntity(request.authorId)

        if (interpretationRepository.existsByAuthorAndSpread(user.id, spreadId)) {
            throw ConflictException("You already have an interpretation for this spread")
        }

        val interpretation =
            Interpretation(
                text = request.text,
                author = user,
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

        if (interpretation.author.id != userId) {
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

        if (interpretation.author.id != userId) {
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
}
