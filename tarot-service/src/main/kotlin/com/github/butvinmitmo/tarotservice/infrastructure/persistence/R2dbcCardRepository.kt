package com.github.butvinmitmo.tarotservice.infrastructure.persistence

import com.github.butvinmitmo.tarotservice.application.interfaces.repository.CardRepository
import com.github.butvinmitmo.tarotservice.domain.model.ArcanaType
import com.github.butvinmitmo.tarotservice.domain.model.Card
import com.github.butvinmitmo.tarotservice.infrastructure.persistence.mapper.ArcanaTypeEntityMapper
import com.github.butvinmitmo.tarotservice.infrastructure.persistence.mapper.CardEntityMapper
import com.github.butvinmitmo.tarotservice.infrastructure.persistence.repository.SpringDataArcanaTypeRepository
import com.github.butvinmitmo.tarotservice.infrastructure.persistence.repository.SpringDataCardRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class R2dbcCardRepository(
    private val springDataCardRepository: SpringDataCardRepository,
    private val springDataArcanaTypeRepository: SpringDataArcanaTypeRepository,
    private val cardEntityMapper: CardEntityMapper,
    private val arcanaTypeEntityMapper: ArcanaTypeEntityMapper,
) : CardRepository {
    override fun findAllPaginated(
        offset: Long,
        limit: Int,
    ): Flux<Card> =
        getArcanaTypeMap()
            .flatMapMany { arcanaTypeMap ->
                springDataCardRepository
                    .findAllPaginated(offset, limit)
                    .map { cardEntity ->
                        val arcanaType = arcanaTypeMap[cardEntity.arcanaTypeId]!!
                        cardEntityMapper.toDomain(cardEntity, arcanaType)
                    }
            }

    override fun findRandomCards(limit: Int): Flux<Card> =
        getArcanaTypeMap()
            .flatMapMany { arcanaTypeMap ->
                springDataCardRepository
                    .findRandomCards(limit)
                    .map { cardEntity ->
                        val arcanaType = arcanaTypeMap[cardEntity.arcanaTypeId]!!
                        cardEntityMapper.toDomain(cardEntity, arcanaType)
                    }
            }

    override fun count(): Mono<Long> = springDataCardRepository.count()

    private fun getArcanaTypeMap(): Mono<Map<UUID, ArcanaType>> =
        springDataArcanaTypeRepository
            .findAll()
            .map { arcanaTypeEntityMapper.toDomain(it) }
            .collectList()
            .map { types -> types.associateBy { it.id } }
}
