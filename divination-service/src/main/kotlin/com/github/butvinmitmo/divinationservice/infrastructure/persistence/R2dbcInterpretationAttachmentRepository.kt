package com.github.butvinmitmo.divinationservice.infrastructure.persistence

import com.github.butvinmitmo.divinationservice.application.interfaces.repository.InterpretationAttachmentRepository
import com.github.butvinmitmo.divinationservice.domain.model.InterpretationAttachment
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.mapper.InterpretationAttachmentEntityMapper
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.repository.SpringDataInterpretationAttachmentRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class R2dbcInterpretationAttachmentRepository(
    private val springDataRepository: SpringDataInterpretationAttachmentRepository,
    private val mapper: InterpretationAttachmentEntityMapper,
) : InterpretationAttachmentRepository {
    override fun save(attachment: InterpretationAttachment): Mono<InterpretationAttachment> =
        springDataRepository
            .save(mapper.toEntity(attachment))
            .map { mapper.toDomain(it) }

    override fun findByInterpretationId(interpretationId: UUID): Mono<InterpretationAttachment> =
        springDataRepository
            .findByInterpretationId(interpretationId)
            .map { mapper.toDomain(it) }

    override fun findByInterpretationIds(interpretationIds: List<UUID>): Flux<InterpretationAttachment> {
        if (interpretationIds.isEmpty()) return Flux.empty()
        return springDataRepository
            .findByInterpretationIdIn(interpretationIds)
            .map { mapper.toDomain(it) }
    }

    override fun deleteByInterpretationId(interpretationId: UUID): Mono<Void> =
        springDataRepository.deleteByInterpretationId(interpretationId)

    override fun existsByInterpretationId(interpretationId: UUID): Mono<Boolean> =
        springDataRepository.existsByInterpretationId(interpretationId)
}
