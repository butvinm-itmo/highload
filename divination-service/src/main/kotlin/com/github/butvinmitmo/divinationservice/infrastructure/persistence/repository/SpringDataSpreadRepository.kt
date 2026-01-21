package com.github.butvinmitmo.divinationservice.infrastructure.persistence.repository

import com.github.butvinmitmo.divinationservice.infrastructure.persistence.entity.SpreadEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface SpringDataSpreadRepository : R2dbcRepository<SpreadEntity, UUID> {
    @Query("SELECT * FROM spread ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    fun findAllOrderByCreatedAtDesc(
        offset: Long,
        limit: Int,
    ): Flux<SpreadEntity>

    @Query("SELECT COUNT(*) FROM spread")
    override fun count(): Mono<Long>

    @Modifying
    @Query("DELETE FROM spread WHERE author_id = :authorId")
    fun deleteByAuthorId(authorId: UUID): Mono<Void>
}
