package com.github.butvinmitmo.filesservice.application.interfaces.repository

import com.github.butvinmitmo.filesservice.domain.model.FileUpload
import com.github.butvinmitmo.filesservice.domain.model.FileUploadStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

interface FileUploadRepository {
    fun save(fileUpload: FileUpload): Mono<FileUpload>

    fun findById(id: UUID): Mono<FileUpload>

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<FileUpload>

    fun updateStatus(
        id: UUID,
        status: FileUploadStatus,
        fileSize: Long?,
        completedAt: Instant?,
    ): Mono<Void>

    fun findExpiredPending(now: Instant): Flux<FileUpload>

    fun deleteById(id: UUID): Mono<Void>
}
