package com.github.butvinmitmo.tarotservice.infrastructure.persistence.repository

import com.github.butvinmitmo.tarotservice.infrastructure.persistence.entity.CardEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface SpringDataCardRepository : R2dbcRepository<CardEntity, UUID> {
    @Query("SELECT * FROM card ORDER BY RANDOM() LIMIT :limit")
    fun findRandomCards(limit: Int): Flux<CardEntity>

    @Query("SELECT * FROM card ORDER BY id LIMIT :limit OFFSET :offset")
    fun findAllPaginated(
        offset: Long,
        limit: Int,
    ): Flux<CardEntity>

    @Query("SELECT COUNT(*) FROM card")
    override fun count(): Mono<Long>
}
