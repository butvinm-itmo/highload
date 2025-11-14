package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.entity.Card
import com.github.butvinmitmo.highload.repository.CardRepository
import org.springframework.stereotype.Service

@Service
class CardService(
    private val cardRepository: CardRepository,
) {
    fun findRandomCards(count: Int): List<Card> = cardRepository.findRandomCards(count)
}
