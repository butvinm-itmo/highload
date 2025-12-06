package com.github.butvinmitmo.tarotservice.service

import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.LayoutTypeDto
import com.github.butvinmitmo.shared.dto.PageResponse
import com.github.butvinmitmo.tarotservice.entity.Card
import com.github.butvinmitmo.tarotservice.entity.LayoutType
import com.github.butvinmitmo.tarotservice.exception.NotFoundException
import com.github.butvinmitmo.tarotservice.mapper.CardMapper
import com.github.butvinmitmo.tarotservice.mapper.LayoutTypeMapper
import com.github.butvinmitmo.tarotservice.repository.CardRepository
import com.github.butvinmitmo.tarotservice.repository.LayoutTypeRepository
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

    fun getLayoutTypeDtoById(id: UUID): LayoutTypeDto {
        val layoutType = getLayoutTypeById(id)
        return layoutTypeMapper.toDto(layoutType)
    }

    fun getRandomCards(count: Int): List<Card> = cardRepository.findRandomCards(count)

    fun getRandomCardDtos(count: Int): List<CardDto> = getRandomCards(count).map { cardMapper.toDto(it) }
}
