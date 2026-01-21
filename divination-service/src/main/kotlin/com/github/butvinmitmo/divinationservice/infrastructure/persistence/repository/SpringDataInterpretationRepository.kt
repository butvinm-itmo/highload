package com.github.butvinmitmo.divinationservice.infrastructure.persistence.repository

import com.github.butvinmitmo.divinationservice.infrastructure.persistence.entity.InterpretationEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface SpringDataInterpretationRepository : R2dbcRepository<InterpretationEntity, UUID> {
    @Query(
        "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END " +
            "FROM interpretation WHERE author_id = :authorId AND spread_id = :spreadId",
    )
    fun existsByAuthorAndSpread(
        authorId: UUID,
        spreadId: UUID,
    ): Mono<Boolean>

    @Query(
        """
        SELECT * FROM interpretation
        WHERE spread_id = :spreadId
        ORDER BY created_at DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun findBySpreadIdOrderByCreatedAtDesc(
        spreadId: UUID,
        offset: Long,
        limit: Int,
    ): Flux<InterpretationEntity>

    @Query("SELECT COUNT(*) FROM interpretation WHERE spread_id = :spreadId")
    fun countBySpreadId(spreadId: UUID): Mono<Long>

    @Modifying
    @Query("DELETE FROM interpretation WHERE author_id = :authorId")
    fun deleteByAuthorId(authorId: UUID): Mono<Void>
}
