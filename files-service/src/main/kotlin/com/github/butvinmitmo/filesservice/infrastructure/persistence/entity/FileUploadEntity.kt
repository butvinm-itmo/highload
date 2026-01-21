package com.github.butvinmitmo.filesservice.infrastructure.persistence.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("file_upload")
data class FileUploadEntity(
    @Id
    val id: UUID? = null,
    @Column("user_id")
    val userId: UUID,
    @Column("file_path")
    val filePath: String,
    @Column("original_file_name")
    val originalFileName: String,
    @Column("content_type")
    val contentType: String,
    @Column("file_size")
    val fileSize: Long?,
    @Column("status")
    val status: String,
    @Column("created_at")
    val createdAt: Instant? = null,
    @Column("expires_at")
    val expiresAt: Instant,
    @Column("completed_at")
    val completedAt: Instant?,
)
