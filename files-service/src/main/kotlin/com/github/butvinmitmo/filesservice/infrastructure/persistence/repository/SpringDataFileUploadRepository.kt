package com.github.butvinmitmo.filesservice.infrastructure.persistence.repository

import com.github.butvinmitmo.filesservice.infrastructure.persistence.entity.FileUploadEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

interface SpringDataFileUploadRepository : R2dbcRepository<FileUploadEntity, UUID> {
    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<FileUploadEntity>

    @Modifying
    @Query(
        """
        UPDATE file_upload
        SET status = :status, file_size = :fileSize, completed_at = :completedAt
        WHERE id = :id
        """,
    )
    fun updateStatus(
        id: UUID,
        status: String,
        fileSize: Long?,
        completedAt: Instant?,
    ): Mono<Long>

    @Query(
        """
        SELECT * FROM file_upload
        WHERE status = 'PENDING' AND expires_at < :now
        """,
    )
    fun findExpiredPending(now: Instant): Flux<FileUploadEntity>
}
