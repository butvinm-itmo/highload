package com.github.butvinmitmo.divinationservice.infrastructure.persistence.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("interpretation")
data class InterpretationEntity(
    @Id
    val id: UUID? = null,
    @Column("text")
    val text: String,
    @Column("author_id")
    val authorId: UUID,
    @Column("spread_id")
    val spreadId: UUID,
    @Column("created_at")
    val createdAt: Instant? = null,
)
