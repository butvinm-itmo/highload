package com.github.butvinmitmo.tarotservice.infrastructure.persistence.repository

import com.github.butvinmitmo.tarotservice.infrastructure.persistence.entity.LayoutTypeEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface SpringDataLayoutTypeRepository : R2dbcRepository<LayoutTypeEntity, UUID> {
    @Query("SELECT * FROM layout_type ORDER BY id LIMIT :limit OFFSET :offset")
    fun findAllPaginated(
        offset: Long,
        limit: Int,
    ): Flux<LayoutTypeEntity>

    @Query("SELECT COUNT(*) FROM layout_type")
    override fun count(): Mono<Long>
}
