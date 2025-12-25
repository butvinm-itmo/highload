package com.github.butvinmitmo.divinationservice.util

import com.github.butvinmitmo.divinationservice.exception.InvalidFileException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Component

@Component
class FileValidator(
    @Value("\${file.max-size-bytes:2097152}") private val maxSizeBytes: Long,
) {
    private val allowedExtensions = setOf("png", "jpg", "jpeg")
    private val allowedContentTypes = setOf("image/png", "image/jpeg")

    fun validate(
        filePart: FilePart,
        fileSize: Long,
    ) {
        validateSize(fileSize)
        validateExtension(filePart.filename())
        validateContentType(filePart.headers().contentType?.toString())
    }

    private fun validateSize(size: Long) {
        if (size > maxSizeBytes) {
            throw InvalidFileException("File size exceeds maximum allowed size of ${maxSizeBytes / 1024 / 1024}MB")
        }
    }

    private fun validateExtension(filename: String) {
        val extension = filename.substringAfterLast('.', "").lowercase()
        if (extension !in allowedExtensions) {
            throw InvalidFileException("Invalid file extension. Allowed: ${allowedExtensions.joinToString(", ")}")
        }
    }

    private fun validateContentType(contentType: String?) {
        if (contentType == null || contentType !in allowedContentTypes) {
            throw InvalidFileException("Invalid content type. Allowed: ${allowedContentTypes.joinToString(", ")}")
        }
    }
}
