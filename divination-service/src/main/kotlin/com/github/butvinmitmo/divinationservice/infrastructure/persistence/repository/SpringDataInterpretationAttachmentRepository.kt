package com.github.butvinmitmo.divinationservice.infrastructure.persistence.repository

import com.github.butvinmitmo.divinationservice.infrastructure.persistence.entity.InterpretationAttachmentEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface SpringDataInterpretationAttachmentRepository : R2dbcRepository<InterpretationAttachmentEntity, UUID> {
    fun findByInterpretationId(interpretationId: UUID): Mono<InterpretationAttachmentEntity>

    @Query("SELECT * FROM interpretation_attachment WHERE interpretation_id IN (:interpretationIds)")
    fun findByInterpretationIdIn(interpretationIds: List<UUID>): Flux<InterpretationAttachmentEntity>

    @Modifying
    @Query("DELETE FROM interpretation_attachment WHERE interpretation_id = :interpretationId")
    fun deleteByInterpretationId(interpretationId: UUID): Mono<Void>

    fun existsByInterpretationId(interpretationId: UUID): Mono<Boolean>
}
