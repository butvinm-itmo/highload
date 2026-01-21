package com.github.butvinmitmo.divinationservice.application.interfaces.repository

import com.github.butvinmitmo.divinationservice.domain.model.InterpretationAttachment
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface InterpretationAttachmentRepository {
    fun save(attachment: InterpretationAttachment): Mono<InterpretationAttachment>

    fun findByInterpretationId(interpretationId: UUID): Mono<InterpretationAttachment>

    fun findByInterpretationIds(interpretationIds: List<UUID>): Flux<InterpretationAttachment>

    fun deleteByInterpretationId(interpretationId: UUID): Mono<Void>

    fun existsByInterpretationId(interpretationId: UUID): Mono<Boolean>
}
