package com.github.butvinmitmo.divinationservice.infrastructure.persistence

import com.github.butvinmitmo.divinationservice.application.interfaces.repository.SpreadRepository
import com.github.butvinmitmo.divinationservice.domain.model.Spread
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.entity.SpreadEntity
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.mapper.SpreadEntityMapper
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.repository.SpringDataSpreadRepository
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
class R2dbcSpreadRepository(
    private val springDataSpreadRepository: SpringDataSpreadRepository,
    private val spreadEntityMapper: SpreadEntityMapper,
    private val databaseClient: DatabaseClient,
) : SpreadRepository {
    override fun save(spread: Spread): Mono<Spread> =
        springDataSpreadRepository
            .save(spreadEntityMapper.toEntity(spread))
            .map { spreadEntityMapper.toDomain(it) }

    override fun findById(id: UUID): Mono<Spread> =
        springDataSpreadRepository
            .findById(id)
            .map { spreadEntityMapper.toDomain(it) }

    override fun findAllOrderByCreatedAtDesc(
        offset: Long,
        limit: Int,
    ): Flux<Spread> =
        springDataSpreadRepository
            .findAllOrderByCreatedAtDesc(offset, limit)
            .map { spreadEntityMapper.toDomain(it) }

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
                SpreadEntity(
                    id = row.get("id", UUID::class.java),
                    question = row.get("question", String::class.java),
                    layoutTypeId = row.get("layout_type_id", UUID::class.java)!!,
                    authorId = row.get("author_id", UUID::class.java)!!,
                    createdAt = row.get("created_at", Instant::class.java),
                )
            }.all()
            .map { spreadEntityMapper.toDomain(it) }

    override fun findLatestSpreads(limit: Int): Flux<Spread> =
        databaseClient
            .sql("SELECT * FROM spread ORDER BY created_at DESC, id DESC LIMIT :limit")
            .bind("limit", limit)
            .map { row, _ ->
                SpreadEntity(
                    id = row.get("id", UUID::class.java),
                    question = row.get("question", String::class.java),
                    layoutTypeId = row.get("layout_type_id", UUID::class.java)!!,
                    authorId = row.get("author_id", UUID::class.java)!!,
                    createdAt = row.get("created_at", Instant::class.java),
                )
            }.all()
            .map { spreadEntityMapper.toDomain(it) }

    override fun count(): Mono<Long> = springDataSpreadRepository.count()

    override fun deleteById(id: UUID): Mono<Void> = springDataSpreadRepository.deleteById(id)

    override fun deleteByAuthorId(authorId: UUID): Mono<Void> = springDataSpreadRepository.deleteByAuthorId(authorId)
}
