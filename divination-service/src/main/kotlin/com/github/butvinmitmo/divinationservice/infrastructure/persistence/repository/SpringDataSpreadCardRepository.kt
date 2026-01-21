package com.github.butvinmitmo.divinationservice.infrastructure.persistence.repository

import com.github.butvinmitmo.divinationservice.infrastructure.persistence.entity.SpreadCardEntity
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.util.UUID

@Repository
interface SpringDataSpreadCardRepository : R2dbcRepository<SpreadCardEntity, UUID> {
    fun findBySpreadId(spreadId: UUID): Flux<SpreadCardEntity>
}
