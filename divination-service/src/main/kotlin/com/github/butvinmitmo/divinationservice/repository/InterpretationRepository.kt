package com.github.butvinmitmo.divinationservice.repository

import com.github.butvinmitmo.divinationservice.entity.Interpretation
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface InterpretationRepository :
    R2dbcRepository<Interpretation, UUID>,
    InterpretationRepositoryCustom {
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
    ): Flux<Interpretation>

    @Query("SELECT COUNT(*) FROM interpretation WHERE spread_id = :spreadId")
    fun countBySpreadId(spreadId: UUID): Mono<Long>

    @Query("DELETE FROM interpretation WHERE author_id = :authorId")
    fun deleteByAuthorId(authorId: UUID): Mono<Void>
}

interface InterpretationRepositoryCustom {
    fun countBySpreadIds(spreadIds: List<UUID>): Flux<Pair<UUID, Long>>
}

@Component
class InterpretationRepositoryCustomImpl(
    private val databaseClient: DatabaseClient,
) : InterpretationRepositoryCustom {
    override fun countBySpreadIds(spreadIds: List<UUID>): Flux<Pair<UUID, Long>> {
        if (spreadIds.isEmpty()) return Flux.empty()

        return databaseClient
            .sql(
                """
                SELECT spread_id, COUNT(*) as count
                FROM interpretation
                WHERE spread_id = ANY(:spreadIds)
                GROUP BY spread_id
                """.trimIndent(),
            ).bind("spreadIds", spreadIds.toTypedArray())
            .map { row, _ ->
                val spreadId = row.get("spread_id", UUID::class.java)!!
                val count = (row.get("count", java.lang.Long::class.java) ?: 0L).toLong()
                spreadId to count
            }.all()
    }
}
