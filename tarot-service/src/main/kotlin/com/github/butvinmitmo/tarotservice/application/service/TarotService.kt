package com.github.butvinmitmo.tarotservice.application.service

import com.github.butvinmitmo.tarotservice.application.interfaces.repository.CardRepository
import com.github.butvinmitmo.tarotservice.application.interfaces.repository.LayoutTypeRepository
import com.github.butvinmitmo.tarotservice.domain.model.Card
import com.github.butvinmitmo.tarotservice.domain.model.LayoutType
import com.github.butvinmitmo.tarotservice.exception.NotFoundException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

data class PageResult<T>(
    val content: List<T>,
    val totalElements: Long,
)

@Service
class TarotService(
    private val cardRepository: CardRepository,
    private val layoutTypeRepository: LayoutTypeRepository,
) {
    fun getCards(
        page: Int,
        size: Int,
    ): Mono<PageResult<Card>> {
        val offset = page.toLong() * size
        return Mono
            .zip(
                cardRepository.findAllPaginated(offset, size).collectList(),
                cardRepository.count(),
            ).map { tuple ->
                PageResult(
                    content = tuple.t1,
                    totalElements = tuple.t2,
                )
            }
    }

    fun getLayoutTypes(
        page: Int,
        size: Int,
    ): Mono<PageResult<LayoutType>> {
        val offset = page.toLong() * size
        return Mono
            .zip(
                layoutTypeRepository.findAllPaginated(offset, size).collectList(),
                layoutTypeRepository.count(),
            ).map { tuple ->
                PageResult(
                    content = tuple.t1,
                    totalElements = tuple.t2,
                )
            }
    }

    fun getLayoutTypeById(id: UUID): Mono<LayoutType> =
        layoutTypeRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Layout type not found")))

    fun getRandomCards(count: Int): Mono<List<Card>> = cardRepository.findRandomCards(count).collectList()
}
