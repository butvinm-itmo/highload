package com.github.butvinmitmo.divinationservice.application.interfaces.repository

import com.github.butvinmitmo.divinationservice.domain.model.SpreadCard
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface SpreadCardRepository {
    fun save(spreadCard: SpreadCard): Mono<SpreadCard>

    fun findBySpreadId(spreadId: UUID): Flux<SpreadCard>
}
