package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.dto.CreateSpreadRequest
import com.github.butvinmitmo.highload.dto.LayoutTypeDto
import com.github.butvinmitmo.highload.dto.PageResponse
import com.github.butvinmitmo.highload.dto.SpreadDto
import com.github.butvinmitmo.highload.dto.SpreadSummaryDto
import com.github.butvinmitmo.highload.entity.LayoutType
import com.github.butvinmitmo.highload.entity.Spread
import com.github.butvinmitmo.highload.entity.SpreadCard
import com.github.butvinmitmo.highload.exception.ForbiddenException
import com.github.butvinmitmo.highload.exception.NotFoundException
import com.github.butvinmitmo.highload.mapper.LayoutTypeMapper
import com.github.butvinmitmo.highload.mapper.SpreadMapper
import com.github.butvinmitmo.highload.repository.LayoutTypeRepository
import com.github.butvinmitmo.highload.repository.SpreadCardRepository
import com.github.butvinmitmo.highload.repository.SpreadRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.random.Random

@Service
class SpreadService(
    private val spreadRepository: SpreadRepository,
    private val spreadCardRepository: SpreadCardRepository,
    private val layoutTypeRepository: LayoutTypeRepository,
    private val userService: UserService,
    private val cardService: CardService,
    private val spreadMapper: SpreadMapper,
    private val layoutTypeMapper: LayoutTypeMapper,
) {
    @Transactional
    fun createSpread(request: CreateSpreadRequest): SpreadDto {
        val user = userService.getUserEntity(request.authorId)

        val layoutType = getLayoutTypeById(request.layoutTypeId)

        val spread =
            Spread(
                question = request.question,
                author = user,
                layoutType = layoutType,
            )
        val savedSpread = spreadRepository.save(spread)

        val cards = cardService.findRandomCards(layoutType.cardsCount)

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

        val completeSpread = spreadRepository.findByIdWithCards(savedSpread.id!!)
            ?: throw IllegalStateException("Spread not found after save")
        return spreadMapper.toDto(completeSpread)
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

    fun getSpreadsByScroll(
        after: UUID?,
        size: Int,
    ): List<SpreadSummaryDto> {
        val spreads =
            if (after != null) {
                spreadRepository.findSpreadsAfterCursor(after, size)
            } else {
                spreadRepository.findLatestSpreads(size)
            }

        return spreads.map { spreadMapper.toSummaryDto(it) }
    }

    fun getSpread(id: UUID): SpreadDto {
        val spread = spreadRepository.findByIdWithCardsAndInterpretations(id)
            ?: throw NotFoundException("Spread not found")

        return spreadMapper.toDto(spread)
    }

    @Transactional
    fun deleteSpread(
        id: UUID,
        userId: UUID,
    ) {
        val spread =
            spreadRepository.findById(id)
                .orElseThrow { NotFoundException("Spread not found") }

        if (spread.author.id != userId) {
            throw ForbiddenException("You can only delete your own spreads")
        }

        spreadRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun getLayoutTypeById(id: UUID): LayoutType {
        return layoutTypeRepository.findById(id)
            .orElseThrow { NotFoundException("Layout type not found") }
    }

    @Transactional(readOnly = true)
    fun getLayoutTypeByName(name: String): LayoutType {
        return layoutTypeRepository.findByName(name)
            ?: throw NotFoundException("Layout type not found: $name")
    }

    @Transactional(readOnly = true)
    fun getAllLayoutTypes(): List<LayoutTypeDto> {
        return layoutTypeRepository.findAll()
            .map { layoutTypeMapper.toDto(it) }
    }

    @Transactional(readOnly = true)
    fun getSpreadEntity(id: UUID): Spread {
        return spreadRepository.findById(id)
            .orElseThrow { NotFoundException("Spread not found") }
    }
}
