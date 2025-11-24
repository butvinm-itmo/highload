package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.dto.CardDto
import com.github.butvinmitmo.highload.dto.PageResponse
import com.github.butvinmitmo.highload.entity.Card
import com.github.butvinmitmo.highload.mapper.CardMapper
import com.github.butvinmitmo.highload.repository.CardRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class CardService(
    private val cardRepository: CardRepository,
    private val cardMapper: CardMapper,
) {
    fun findRandomCards(count: Int): List<Card> = cardRepository.findRandomCards(count)

    fun getCards(
        page: Int,
        size: Int,
    ): PageResponse<CardDto> {
        val pageable = PageRequest.of(page, size)
        val cardsPage = cardRepository.findAll(pageable)
        return PageResponse(
            content = cardsPage.content.map { cardMapper.toDto(it) },
            page = cardsPage.number,
            size = cardsPage.size,
            totalElements = cardsPage.totalElements,
            totalPages = cardsPage.totalPages,
            isFirst = cardsPage.isFirst,
            isLast = cardsPage.isLast,
        )
    }
}
