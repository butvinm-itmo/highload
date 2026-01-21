package com.github.butvinmitmo.filesservice.infrastructure.persistence.mapper

import com.github.butvinmitmo.filesservice.domain.model.FileUpload
import com.github.butvinmitmo.filesservice.domain.model.FileUploadStatus
import com.github.butvinmitmo.filesservice.infrastructure.persistence.entity.FileUploadEntity
import org.springframework.stereotype.Component

@Component
class FileUploadEntityMapper {
    fun toDomain(entity: FileUploadEntity): FileUpload =
        FileUpload(
            id = entity.id!!,
            userId = entity.userId,
            filePath = entity.filePath,
            originalFileName = entity.originalFileName,
            contentType = entity.contentType,
            fileSize = entity.fileSize,
            status = FileUploadStatus.valueOf(entity.status),
            createdAt = entity.createdAt!!,
            expiresAt = entity.expiresAt,
            completedAt = entity.completedAt,
        )

    fun toEntity(fileUpload: FileUpload): FileUploadEntity =
        FileUploadEntity(
            id = fileUpload.id,
            userId = fileUpload.userId,
            filePath = fileUpload.filePath,
            originalFileName = fileUpload.originalFileName,
            contentType = fileUpload.contentType,
            fileSize = fileUpload.fileSize,
            status = fileUpload.status.name,
            createdAt = fileUpload.createdAt,
            expiresAt = fileUpload.expiresAt,
            completedAt = fileUpload.completedAt,
        )
}
