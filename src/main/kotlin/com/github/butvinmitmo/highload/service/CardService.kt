package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.entity.Card
import com.github.butvinmitmo.highload.repository.CardRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CardService(
    private val cardRepository: CardRepository,
) {
    @Transactional(readOnly = true)
    fun findRandomCards(count: Int): List<Card> = cardRepository.findRandomCards(count)
}
