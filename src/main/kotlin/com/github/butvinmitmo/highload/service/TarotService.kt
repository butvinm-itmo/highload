package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.dto.CardDto
import com.github.butvinmitmo.highload.dto.LayoutTypeDto
import com.github.butvinmitmo.highload.dto.PageResponse
import com.github.butvinmitmo.highload.entity.Card
import com.github.butvinmitmo.highload.entity.LayoutType
import com.github.butvinmitmo.highload.exception.NotFoundException
import com.github.butvinmitmo.highload.mapper.CardMapper
import com.github.butvinmitmo.highload.mapper.LayoutTypeMapper
import com.github.butvinmitmo.highload.repository.CardRepository
import com.github.butvinmitmo.highload.repository.LayoutTypeRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TarotService(
    private val cardRepository: CardRepository,
    private val layoutTypeRepository: LayoutTypeRepository,
    private val cardMapper: CardMapper,
    private val layoutTypeMapper: LayoutTypeMapper,
) {
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

    fun getLayoutTypes(
        page: Int,
        size: Int,
    ): PageResponse<LayoutTypeDto> {
        val pageable = PageRequest.of(page, size)
        val layoutTypesPage = layoutTypeRepository.findAll(pageable)
        return PageResponse(
            content = layoutTypesPage.content.map { layoutTypeMapper.toDto(it) },
            page = layoutTypesPage.number,
            size = layoutTypesPage.size,
            totalElements = layoutTypesPage.totalElements,
            totalPages = layoutTypesPage.totalPages,
            isFirst = layoutTypesPage.isFirst,
            isLast = layoutTypesPage.isLast,
        )
    }

    fun getLayoutTypeById(id: UUID): LayoutType =
        layoutTypeRepository
            .findById(id)
            .orElseThrow { NotFoundException("Layout type not found") }

    fun getRandomCards(count: Int): List<Card> = cardRepository.findRandomCards(count)
}
