package com.github.butvinmitmo.tarotservice.repository

import com.github.butvinmitmo.tarotservice.entity.Card
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface CardRepository : R2dbcRepository<Card, UUID> {
    @Query("SELECT * FROM card ORDER BY RANDOM() LIMIT :limit")
    fun findRandomCards(limit: Int): Flux<Card>

    @Query("SELECT * FROM card ORDER BY id LIMIT :limit OFFSET :offset")
    fun findAllPaginated(
        offset: Long,
        limit: Int,
    ): Flux<Card>

    @Query("SELECT COUNT(*) FROM card")
    override fun count(): Mono<Long>
}
