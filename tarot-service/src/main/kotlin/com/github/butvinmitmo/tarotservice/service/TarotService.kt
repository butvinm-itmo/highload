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
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
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
    ): Mono<PageResponse<CardDto>> =
        Mono
            .fromCallable {
                val pageable = PageRequest.of(page, size)
                val cardsPage = cardRepository.findAll(pageable)
                PageResponse(
                    content = cardsPage.content.map { cardMapper.toDto(it) },
                    page = cardsPage.number,
                    size = cardsPage.size,
                    totalElements = cardsPage.totalElements,
                    totalPages = cardsPage.totalPages,
                    isFirst = cardsPage.isFirst,
                    isLast = cardsPage.isLast,
                )
            }.subscribeOn(Schedulers.boundedElastic())

    fun getLayoutTypes(
        page: Int,
        size: Int,
    ): Mono<PageResponse<LayoutTypeDto>> =
        Mono
            .fromCallable {
                val pageable = PageRequest.of(page, size)
                val layoutTypesPage = layoutTypeRepository.findAll(pageable)
                PageResponse(
                    content = layoutTypesPage.content.map { layoutTypeMapper.toDto(it) },
                    page = layoutTypesPage.number,
                    size = layoutTypesPage.size,
                    totalElements = layoutTypesPage.totalElements,
                    totalPages = layoutTypesPage.totalPages,
                    isFirst = layoutTypesPage.isFirst,
                    isLast = layoutTypesPage.isLast,
                )
            }.subscribeOn(Schedulers.boundedElastic())

    fun getLayoutTypeById(id: UUID): Mono<LayoutType> =
        Mono
            .fromCallable {
                layoutTypeRepository
                    .findById(id)
                    .orElseThrow { NotFoundException("Layout type not found") }
            }.subscribeOn(Schedulers.boundedElastic())

    fun getLayoutTypeDtoById(id: UUID): Mono<LayoutTypeDto> =
        getLayoutTypeById(id)
            .map { layoutTypeMapper.toDto(it) }

    fun getRandomCards(count: Int): Mono<List<Card>> =
        Mono
            .fromCallable {
                cardRepository.findRandomCards(count)
            }.subscribeOn(Schedulers.boundedElastic())

    fun getRandomCardDtos(count: Int): Mono<List<CardDto>> =
        getRandomCards(count)
            .map { cards -> cards.map { cardMapper.toDto(it) } }
}
