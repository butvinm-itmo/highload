package com.github.butvinmitmo.divinationservice.repository

import com.github.butvinmitmo.divinationservice.entity.Spread
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
interface SpreadRepository :
    R2dbcRepository<Spread, UUID>,
    SpreadRepositoryCustom {
    @Query("SELECT * FROM spread WHERE id = :id")
    fun findByIdWithCards(id: UUID): Mono<Spread>

    @Query("SELECT * FROM spread ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    fun findAllOrderByCreatedAtDesc(
        offset: Long,
        limit: Int,
    ): Flux<Spread>

    @Query("SELECT COUNT(*) FROM spread")
    override fun count(): Mono<Long>
}

interface SpreadRepositoryCustom {
    fun findSpreadsAfterCursor(
        spreadId: UUID,
        limit: Int,
    ): Flux<Spread>

    fun findLatestSpreads(limit: Int): Flux<Spread>
}

@Component
class SpreadRepositoryCustomImpl(
    private val databaseClient: DatabaseClient,
) : SpreadRepositoryCustom {
    override fun findSpreadsAfterCursor(
        spreadId: UUID,
        limit: Int,
    ): Flux<Spread> =
        databaseClient
            .sql(
                """
                SELECT * FROM spread
                WHERE created_at < (SELECT created_at FROM spread WHERE id = :spreadId)
                OR (created_at = (SELECT created_at FROM spread WHERE id = :spreadId) AND id < :spreadId)
                ORDER BY created_at DESC, id DESC
                LIMIT :limit
                """.trimIndent(),
            ).bind("spreadId", spreadId)
            .bind("limit", limit)
            .map { row, _ ->
                Spread(
                    id = row.get("id", UUID::class.java),
                    question = row.get("question", String::class.java),
                    layoutTypeId = row.get("layout_type_id", UUID::class.java)!!,
                    authorId = row.get("author_id", UUID::class.java)!!,
                    createdAt = row.get("created_at", Instant::class.java),
                )
            }.all()

    override fun findLatestSpreads(limit: Int): Flux<Spread> =
        databaseClient
            .sql("SELECT * FROM spread ORDER BY created_at DESC, id DESC LIMIT :limit")
            .bind("limit", limit)
            .map { row, _ ->
                Spread(
                    id = row.get("id", UUID::class.java),
                    question = row.get("question", String::class.java),
                    layoutTypeId = row.get("layout_type_id", UUID::class.java)!!,
                    authorId = row.get("author_id", UUID::class.java)!!,
                    createdAt = row.get("created_at", Instant::class.java),
                )
            }.all()
}
