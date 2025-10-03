package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.dto.CreateSpreadRequest
import com.github.butvinmitmo.highload.dto.PageResponse
import com.github.butvinmitmo.highload.dto.SpreadDto
import com.github.butvinmitmo.highload.dto.SpreadSummaryDto
import com.github.butvinmitmo.highload.entity.Spread
import com.github.butvinmitmo.highload.entity.SpreadCard
import com.github.butvinmitmo.highload.exception.ForbiddenException
import com.github.butvinmitmo.highload.exception.NotFoundException
import com.github.butvinmitmo.highload.mapper.SpreadMapper
import com.github.butvinmitmo.highload.repository.CardRepository
import com.github.butvinmitmo.highload.repository.LayoutTypeRepository
import com.github.butvinmitmo.highload.repository.SpreadCardRepository
import com.github.butvinmitmo.highload.repository.SpreadRepository
import com.github.butvinmitmo.highload.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.random.Random

@Service
class SpreadService(
    private val spreadRepository: SpreadRepository,
    private val userRepository: UserRepository,
    private val layoutTypeRepository: LayoutTypeRepository,
    private val cardRepository: CardRepository,
    private val spreadCardRepository: SpreadCardRepository,
    private val spreadMapper: SpreadMapper,
) {
    @Transactional
    fun createSpread(request: CreateSpreadRequest): SpreadDto {
        // 1. Validate user exists
        val user =
            userRepository.findById(request.authorId)
                ?: throw NotFoundException("User not found")

        // 2. Get layout type by name (convert UUID to name lookup)
        // For now, we'll use a hardcoded mapping or fetch by name
        val layoutType =
            layoutTypeRepository.findByName("THREE_CARDS") // TODO: fix this based on ID
                ?: throw NotFoundException("Layout type not found")

        // 3. Create spread entity
        val spread =
            Spread(
                question = request.question,
                author = user,
                layoutType = layoutType,
            )
        val savedSpread = spreadRepository.save(spread)

        // 4. Generate random cards based on layout
        val cards = cardRepository.findRandomCards(layoutType.cardsCount)

        // 5. Create spread-card relationships
        cards.forEachIndexed { index, card ->
            val spreadCard =
                SpreadCard(
                    spread = savedSpread,
                    card = card,
                    positionInSpread = index,
                    isReversed = Random.nextBoolean(),
                )
            spreadCardRepository.save(spreadCard)
        }

        // 6. Fetch complete spread with cards and return
        val spreadCards = spreadCardRepository.findBySpreadId(savedSpread.id!!)
        return spreadMapper.toDto(savedSpread, spreadCards)
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
        val spread =
            spreadRepository.findById(id)
                ?: throw NotFoundException("Spread not found")

        val spreadCards = spreadCardRepository.findBySpreadId(id)
        val interpretations = emptyList<com.github.butvinmitmo.highload.entity.Interpretation>() // Will be fetched when needed

        return spreadMapper.toDto(spread, spreadCards, interpretations)
    }

    @Transactional
    fun deleteSpread(
        id: UUID,
        userId: UUID,
    ) {
        val spread =
            spreadRepository.findById(id)
                ?: throw NotFoundException("Spread not found")

        if (spread.author.id != userId) {
            throw ForbiddenException("You can only delete your own spreads")
        }

        // Database CASCADE DELETE handles interpretations and spread_cards automatically
        spreadRepository.deleteById(id)
    }
}
