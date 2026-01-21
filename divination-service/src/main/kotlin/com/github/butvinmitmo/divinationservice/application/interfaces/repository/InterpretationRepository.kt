package com.github.butvinmitmo.divinationservice.application.interfaces.repository

import com.github.butvinmitmo.divinationservice.domain.model.Interpretation
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface InterpretationRepository {
    fun save(interpretation: Interpretation): Mono<Interpretation>

    fun findById(id: UUID): Mono<Interpretation>

    fun findBySpreadIdOrderByCreatedAtDesc(
        spreadId: UUID,
        offset: Long,
        limit: Int,
    ): Flux<Interpretation>

    fun existsByAuthorAndSpread(
        authorId: UUID,
        spreadId: UUID,
    ): Mono<Boolean>

    fun countBySpreadId(spreadId: UUID): Mono<Long>

    fun countBySpreadIds(spreadIds: List<UUID>): Flux<Pair<UUID, Long>>

    fun deleteById(id: UUID): Mono<Void>

    fun deleteByAuthorId(authorId: UUID): Mono<Void>
}
