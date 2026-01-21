package com.github.butvinmitmo.tarotservice.application.interfaces.repository

import com.github.butvinmitmo.tarotservice.domain.model.Card
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface CardRepository {
    fun findAllPaginated(
        offset: Long,
        limit: Int,
    ): Flux<Card>

    fun findRandomCards(limit: Int): Flux<Card>

    fun count(): Mono<Long>
}
