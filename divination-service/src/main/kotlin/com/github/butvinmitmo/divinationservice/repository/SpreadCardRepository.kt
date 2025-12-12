package com.github.butvinmitmo.divinationservice.repository

import com.github.butvinmitmo.divinationservice.entity.SpreadCard
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.util.UUID

@Repository
interface SpreadCardRepository : R2dbcRepository<SpreadCard, UUID> {
    fun findBySpreadId(spreadId: UUID): Flux<SpreadCard>
}
