package com.github.butvinmitmo.tarotservice.service

import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.LayoutTypeDto
import com.github.butvinmitmo.shared.dto.PageResponse
import com.github.butvinmitmo.tarotservice.entity.ArcanaType
import com.github.butvinmitmo.tarotservice.entity.Card
import com.github.butvinmitmo.tarotservice.entity.LayoutType
import com.github.butvinmitmo.tarotservice.exception.NotFoundException
import com.github.butvinmitmo.tarotservice.mapper.CardMapper
import com.github.butvinmitmo.tarotservice.mapper.LayoutTypeMapper
import com.github.butvinmitmo.tarotservice.repository.ArcanaTypeRepository
import com.github.butvinmitmo.tarotservice.repository.CardRepository
import com.github.butvinmitmo.tarotservice.repository.LayoutTypeRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class TarotService(
    private val cardRepository: CardRepository,
    private val layoutTypeRepository: LayoutTypeRepository,
    private val arcanaTypeRepository: ArcanaTypeRepository,
    private val cardMapper: CardMapper,
    private val layoutTypeMapper: LayoutTypeMapper,
) {
    fun getCards(
        page: Int,
        size: Int,
    ): Mono<PageResponse<CardDto>> {
        val offset = page.toLong() * size
        return Mono
            .zip(
                cardRepository.findAllPaginated(offset, size).collectList(),
                cardRepository.count(),
                getArcanaTypeMap(),
            ).map { tuple ->
                val cards = tuple.t1
                val totalElements = tuple.t2
                val arcanaTypeMap = tuple.t3
                val totalPages = if (totalElements == 0L) 0 else ((totalElements - 1) / size + 1).toInt()
                PageResponse(
                    content = cards.map { card -> cardMapper.toDto(card, arcanaTypeMap[card.arcanaTypeId]!!) },
                    page = page,
                    size = cards.size,
                    totalElements = totalElements,
                    totalPages = totalPages,
                    isFirst = page == 0,
                    isLast = page >= totalPages - 1,
                )
            }
    }

    fun getLayoutTypes(
        page: Int,
        size: Int,
    ): Mono<PageResponse<LayoutTypeDto>> {
        val offset = page.toLong() * size
        return Mono
            .zip(
                layoutTypeRepository.findAllPaginated(offset, size).collectList(),
                layoutTypeRepository.count(),
            ).map { tuple ->
                val layoutTypes = tuple.t1
                val totalElements = tuple.t2
                val totalPages = if (totalElements == 0L) 0 else ((totalElements - 1) / size + 1).toInt()
                PageResponse(
                    content = layoutTypes.map { layoutTypeMapper.toDto(it) },
                    page = page,
                    size = layoutTypes.size,
                    totalElements = totalElements,
                    totalPages = totalPages,
                    isFirst = page == 0,
                    isLast = page >= totalPages - 1,
                )
            }
    }

    fun getLayoutTypeById(id: UUID): Mono<LayoutType> =
        layoutTypeRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Layout type not found")))

    fun getLayoutTypeDtoById(id: UUID): Mono<LayoutTypeDto> =
        getLayoutTypeById(id)
            .map { layoutTypeMapper.toDto(it) }

    fun getRandomCards(count: Int): Mono<List<Card>> = cardRepository.findRandomCards(count).collectList()

    fun getRandomCardDtos(count: Int): Mono<List<CardDto>> =
        Mono
            .zip(
                getRandomCards(count),
                getArcanaTypeMap(),
            ).map { tuple ->
                val cards = tuple.t1
                val arcanaTypeMap = tuple.t2
                cards.map { card -> cardMapper.toDto(card, arcanaTypeMap[card.arcanaTypeId]!!) }
            }

    private fun getArcanaTypeMap(): Mono<Map<UUID, ArcanaType>> =
        arcanaTypeRepository
            .findAll()
            .collectList()
            .map { types -> types.associateBy { it.id!! } }
}
