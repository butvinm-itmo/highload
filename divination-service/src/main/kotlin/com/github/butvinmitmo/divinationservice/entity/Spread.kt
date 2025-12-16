package com.github.butvinmitmo.divinationservice.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("spread")
data class Spread(
    @Id
    val id: UUID? = null,
    @Column("question")
    val question: String?,
    @Column("layout_type_id")
    val layoutTypeId: UUID,
    @Column("author_id")
    val authorId: UUID,
    @Column("created_at")
    val createdAt: Instant? = null,
)
