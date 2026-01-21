package com.github.butvinmitmo.filesservice.infrastructure.messaging.mapper

import com.github.butvinmitmo.filesservice.domain.model.FileUpload
import com.github.butvinmitmo.shared.dto.events.FileEventData
import org.springframework.stereotype.Component

@Component
class FileEventDataMapper {
    fun toEventData(fileUpload: FileUpload): FileEventData =
        FileEventData(
            uploadId = fileUpload.id!!,
            filePath = fileUpload.filePath,
            originalFileName = fileUpload.originalFileName,
            contentType = fileUpload.contentType,
            fileSize = fileUpload.fileSize,
            userId = fileUpload.userId,
            completedAt = fileUpload.completedAt,
        )
}
