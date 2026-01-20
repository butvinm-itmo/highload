package com.github.butvinmitmo.divinationservice.infrastructure.persistence

import com.github.butvinmitmo.divinationservice.application.interfaces.repository.SpreadCardRepository
import com.github.butvinmitmo.divinationservice.domain.model.SpreadCard
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.mapper.SpreadCardEntityMapper
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.repository.SpringDataSpreadCardRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class R2dbcSpreadCardRepository(
    private val springDataSpreadCardRepository: SpringDataSpreadCardRepository,
    private val spreadCardEntityMapper: SpreadCardEntityMapper,
) : SpreadCardRepository {
    override fun save(spreadCard: SpreadCard): Mono<SpreadCard> =
        springDataSpreadCardRepository
            .save(spreadCardEntityMapper.toEntity(spreadCard))
            .map { spreadCardEntityMapper.toDomain(it) }

    override fun findBySpreadId(spreadId: UUID): Flux<SpreadCard> =
        springDataSpreadCardRepository
            .findBySpreadId(spreadId)
            .map { spreadCardEntityMapper.toDomain(it) }
}
