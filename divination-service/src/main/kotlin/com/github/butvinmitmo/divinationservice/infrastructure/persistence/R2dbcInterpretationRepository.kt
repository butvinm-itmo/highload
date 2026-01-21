package com.github.butvinmitmo.divinationservice.infrastructure.persistence

import com.github.butvinmitmo.divinationservice.application.interfaces.repository.InterpretationRepository
import com.github.butvinmitmo.divinationservice.domain.model.Interpretation
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.mapper.InterpretationEntityMapper
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.repository.SpringDataInterpretationRepository
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class R2dbcInterpretationRepository(
    private val springDataInterpretationRepository: SpringDataInterpretationRepository,
    private val interpretationEntityMapper: InterpretationEntityMapper,
    private val databaseClient: DatabaseClient,
) : InterpretationRepository {
    override fun save(interpretation: Interpretation): Mono<Interpretation> =
        springDataInterpretationRepository
            .save(interpretationEntityMapper.toEntity(interpretation))
            .map { interpretationEntityMapper.toDomain(it) }

    override fun findById(id: UUID): Mono<Interpretation> =
        springDataInterpretationRepository
            .findById(id)
            .map { interpretationEntityMapper.toDomain(it) }

    override fun findBySpreadIdOrderByCreatedAtDesc(
        spreadId: UUID,
        offset: Long,
        limit: Int,
    ): Flux<Interpretation> =
        springDataInterpretationRepository
            .findBySpreadIdOrderByCreatedAtDesc(spreadId, offset, limit)
            .map { interpretationEntityMapper.toDomain(it) }

    override fun existsByAuthorAndSpread(
        authorId: UUID,
        spreadId: UUID,
    ): Mono<Boolean> = springDataInterpretationRepository.existsByAuthorAndSpread(authorId, spreadId)

    override fun countBySpreadId(spreadId: UUID): Mono<Long> =
        springDataInterpretationRepository.countBySpreadId(spreadId)

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

    override fun deleteById(id: UUID): Mono<Void> = springDataInterpretationRepository.deleteById(id)

    override fun deleteByAuthorId(authorId: UUID): Mono<Void> =
        springDataInterpretationRepository.deleteByAuthorId(authorId)
}
