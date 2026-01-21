package com.github.butvinmitmo.tarotservice.infrastructure.persistence

import com.github.butvinmitmo.tarotservice.application.interfaces.repository.LayoutTypeRepository
import com.github.butvinmitmo.tarotservice.domain.model.LayoutType
import com.github.butvinmitmo.tarotservice.infrastructure.persistence.mapper.LayoutTypeEntityMapper
import com.github.butvinmitmo.tarotservice.infrastructure.persistence.repository.SpringDataLayoutTypeRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class R2dbcLayoutTypeRepository(
    private val springDataLayoutTypeRepository: SpringDataLayoutTypeRepository,
    private val layoutTypeEntityMapper: LayoutTypeEntityMapper,
) : LayoutTypeRepository {
    override fun findById(id: UUID): Mono<LayoutType> =
        springDataLayoutTypeRepository
            .findById(id)
            .map { layoutTypeEntityMapper.toDomain(it) }

    override fun findAllPaginated(
        offset: Long,
        limit: Int,
    ): Flux<LayoutType> =
        springDataLayoutTypeRepository
            .findAllPaginated(offset, limit)
            .map { layoutTypeEntityMapper.toDomain(it) }

    override fun count(): Mono<Long> = springDataLayoutTypeRepository.count()
}
