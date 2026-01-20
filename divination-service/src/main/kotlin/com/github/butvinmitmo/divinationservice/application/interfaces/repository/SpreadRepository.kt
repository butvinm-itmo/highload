package com.github.butvinmitmo.divinationservice.application.interfaces.repository

import com.github.butvinmitmo.divinationservice.domain.model.Spread
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface SpreadRepository {
    fun save(spread: Spread): Mono<Spread>

    fun findById(id: UUID): Mono<Spread>

    fun findAllOrderByCreatedAtDesc(
        offset: Long,
        limit: Int,
    ): Flux<Spread>

    fun findSpreadsAfterCursor(
        spreadId: UUID,
        limit: Int,
    ): Flux<Spread>

    fun findLatestSpreads(limit: Int): Flux<Spread>

    fun count(): Mono<Long>

    fun deleteById(id: UUID): Mono<Void>

    fun deleteByAuthorId(authorId: UUID): Mono<Void>
}
