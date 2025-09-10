package com.itmo.tarot.service

import com.itmo.tarot.entity.Card
import com.itmo.tarot.entity.LayoutType
import com.itmo.tarot.repository.CardRepository
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class CardService(
    private val cardRepository: CardRepository
) {
    
    fun generateCardsForLayout(layoutType: LayoutType): List<Card> {
        val allCards = cardRepository.findAll()
        val cardCount = when (layoutType) {
            LayoutType.ONE_CARD -> 1
            LayoutType.THREE_CARDS -> 3
            LayoutType.CROSS -> 5
        }
        
        return allCards.shuffled(Random.Default).take(cardCount)
    }
    
    fun findById(id: Int): Card {
        return cardRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Card with id $id not found") }
    }
    
    fun findAll(): List<Card> = cardRepository.findAll()
}