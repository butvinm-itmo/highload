package com.itmo.tarot.service

import com.itmo.tarot.dto.request.CreateSpreadRequest
import com.itmo.tarot.dto.response.CardResponse
import com.itmo.tarot.dto.response.InterpretationResponse
import com.itmo.tarot.dto.response.SpreadListResponse
import com.itmo.tarot.dto.response.SpreadResponse
import com.itmo.tarot.entity.Spread
import com.itmo.tarot.entity.SpreadCard
import com.itmo.tarot.exception.SpreadNotFoundException
import com.itmo.tarot.exception.UnauthorizedOperationException
import com.itmo.tarot.repository.SpreadRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

@Service
class SpreadService(
    private val spreadRepository: SpreadRepository,
    private val userService: UserService,
    private val cardService: CardService
) {
    
    @Transactional
    fun createSpread(request: CreateSpreadRequest): SpreadResponse {
        val user = userService.findOrCreateUser(request.userId)
        val selectedCards = cardService.generateCardsForLayout(request.layoutType)
        
        val spread = Spread(
            question = request.question,
            layoutType = request.layoutType,
            author = user
        )
        
        val savedSpread = spreadRepository.save(spread)
        
        selectedCards.forEachIndexed { index, card ->
            val spreadCard = SpreadCard(
                spread = savedSpread,
                card = card,
                positionInSpread = index + 1,
                isReversed = Random.nextBoolean()
            )
            savedSpread.spreadCards.add(spreadCard)
        }
        
        spreadRepository.save(savedSpread)
        
        return mapToSpreadResponse(savedSpread)
    }
    
    fun findById(id: Long): SpreadResponse {
        val spread = spreadRepository.findByIdWithDetails(id)
            ?: throw SpreadNotFoundException(id)
        return mapToSpreadResponse(spread)
    }
    
    fun findAll(pageable: Pageable): Page<SpreadListResponse> {
        return spreadRepository.findAll(pageable)
            .map { mapToSpreadListResponse(it) }
    }
    
    fun findAllForInfiniteScroll(afterId: Long?, pageable: Pageable): List<SpreadListResponse> {
        return spreadRepository.findAllForInfiniteScroll(afterId, pageable)
            .map { mapToSpreadListResponse(it) }
    }
    
    @Transactional
    fun deleteSpread(id: Long, userId: Long) {
        val spread = spreadRepository.findById(id)
            .orElseThrow { SpreadNotFoundException(id) }
        
        if (spread.author.id != userId) {
            throw UnauthorizedOperationException("User $userId is not authorized to delete spread $id")
        }
        
        spreadRepository.deleteById(id)
    }
    
    private fun mapToSpreadResponse(spread: Spread): SpreadResponse {
        val cards = spread.spreadCards
            .sortedBy { it.positionInSpread }
            .map { spreadCard ->
                CardResponse(
                    id = spreadCard.card.id,
                    name = spreadCard.card.name,
                    arcanaType = spreadCard.card.arcanaType,
                    position = spreadCard.positionInSpread,
                    isReversed = spreadCard.isReversed
                )
            }
        
        val interpretations = spread.interpretations.map { interpretation ->
            InterpretationResponse(
                id = interpretation.id!!,
                text = interpretation.text,
                createdAt = interpretation.createdAt,
                authorId = interpretation.author.id
            )
        }
        
        return SpreadResponse(
            id = spread.id!!,
            question = spread.question,
            layoutType = spread.layoutType,
            createdAt = spread.createdAt,
            authorId = spread.author.id,
            cards = cards,
            interpretations = interpretations
        )
    }
    
    private fun mapToSpreadListResponse(spread: Spread): SpreadListResponse {
        return SpreadListResponse(
            id = spread.id!!,
            question = spread.question,
            layoutType = spread.layoutType,
            createdAt = spread.createdAt,
            authorId = spread.author.id
        )
    }
}