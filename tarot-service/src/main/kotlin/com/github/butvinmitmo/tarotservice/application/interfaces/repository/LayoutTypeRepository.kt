package com.github.butvinmitmo.tarotservice.application.interfaces.repository

import com.github.butvinmitmo.tarotservice.domain.model.LayoutType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface LayoutTypeRepository {
    fun findById(id: UUID): Mono<LayoutType>

    fun findAllPaginated(
        offset: Long,
        limit: Int,
    ): Flux<LayoutType>

    fun count(): Mono<Long>
}
