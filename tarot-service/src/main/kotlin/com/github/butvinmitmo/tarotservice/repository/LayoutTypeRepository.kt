package com.github.butvinmitmo.tarotservice.repository

import com.github.butvinmitmo.tarotservice.entity.LayoutType
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface LayoutTypeRepository : R2dbcRepository<LayoutType, UUID> {
    fun findByName(name: String): Mono<LayoutType>

    @Query("SELECT * FROM layout_type ORDER BY id LIMIT :limit OFFSET :offset")
    fun findAllPaginated(
        offset: Long,
        limit: Int,
    ): Flux<LayoutType>

    @Query("SELECT COUNT(*) FROM layout_type")
    override fun count(): Mono<Long>
}
