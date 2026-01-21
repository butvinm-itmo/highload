package com.github.butvinmitmo.filesservice.infrastructure.persistence

import com.github.butvinmitmo.filesservice.application.interfaces.repository.FileUploadRepository
import com.github.butvinmitmo.filesservice.domain.model.FileUpload
import com.github.butvinmitmo.filesservice.domain.model.FileUploadStatus
import com.github.butvinmitmo.filesservice.infrastructure.persistence.mapper.FileUploadEntityMapper
import com.github.butvinmitmo.filesservice.infrastructure.persistence.repository.SpringDataFileUploadRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
class R2dbcFileUploadRepository(
    private val springDataFileUploadRepository: SpringDataFileUploadRepository,
    private val fileUploadEntityMapper: FileUploadEntityMapper,
) : FileUploadRepository {
    override fun save(fileUpload: FileUpload): Mono<FileUpload> {
        val entity = fileUploadEntityMapper.toEntity(fileUpload)
        return springDataFileUploadRepository
            .save(entity)
            .flatMap { savedEntity ->
                // Re-fetch to get database-generated fields (id, created_at)
                springDataFileUploadRepository
                    .findById(savedEntity.id!!)
                    .map { fileUploadEntityMapper.toDomain(it) }
            }
    }

    override fun findById(id: UUID): Mono<FileUpload> =
        springDataFileUploadRepository.findById(id).map { fileUploadEntityMapper.toDomain(it) }

    override fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<FileUpload> =
        springDataFileUploadRepository
            .findByIdAndUserId(id, userId)
            .map { fileUploadEntityMapper.toDomain(it) }

    override fun updateStatus(
        id: UUID,
        status: FileUploadStatus,
        fileSize: Long?,
        completedAt: Instant?,
    ): Mono<Void> =
        springDataFileUploadRepository
            .updateStatus(id, status.name, fileSize, completedAt)
            .then()

    override fun findExpiredPending(now: Instant): Flux<FileUpload> =
        springDataFileUploadRepository.findExpiredPending(now).map { fileUploadEntityMapper.toDomain(it) }

    override fun deleteById(id: UUID): Mono<Void> = springDataFileUploadRepository.deleteById(id)
}
