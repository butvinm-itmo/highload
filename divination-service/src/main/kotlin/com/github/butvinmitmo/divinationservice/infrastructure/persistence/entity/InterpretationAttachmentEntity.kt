package com.github.butvinmitmo.divinationservice.infrastructure.persistence.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("interpretation_attachment")
data class InterpretationAttachmentEntity(
    @Id
    val id: UUID? = null,
    @Column("interpretation_id")
    val interpretationId: UUID,
    @Column("file_upload_id")
    val fileUploadId: UUID,
    @Column("original_file_name")
    val originalFileName: String,
    @Column("content_type")
    val contentType: String,
    @Column("file_size")
    val fileSize: Long,
    @Column("created_at")
    val createdAt: Instant? = null,
)
