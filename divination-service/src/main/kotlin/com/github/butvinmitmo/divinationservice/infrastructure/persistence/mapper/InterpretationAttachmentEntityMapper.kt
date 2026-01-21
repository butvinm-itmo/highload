package com.github.butvinmitmo.divinationservice.infrastructure.persistence.mapper

import com.github.butvinmitmo.divinationservice.domain.model.InterpretationAttachment
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.entity.InterpretationAttachmentEntity
import org.springframework.stereotype.Component

@Component
class InterpretationAttachmentEntityMapper {
    fun toDomain(entity: InterpretationAttachmentEntity): InterpretationAttachment =
        InterpretationAttachment(
            id = entity.id,
            interpretationId = entity.interpretationId,
            fileUploadId = entity.fileUploadId,
            originalFileName = entity.originalFileName,
            contentType = entity.contentType,
            fileSize = entity.fileSize,
            createdAt = entity.createdAt,
        )

    fun toEntity(domain: InterpretationAttachment): InterpretationAttachmentEntity =
        InterpretationAttachmentEntity(
            id = domain.id,
            interpretationId = domain.interpretationId,
            fileUploadId = domain.fileUploadId,
            originalFileName = domain.originalFileName,
            contentType = domain.contentType,
            fileSize = domain.fileSize,
            createdAt = domain.createdAt,
        )
}
