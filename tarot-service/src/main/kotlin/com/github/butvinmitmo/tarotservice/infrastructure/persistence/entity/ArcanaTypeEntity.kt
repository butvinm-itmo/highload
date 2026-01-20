package com.github.butvinmitmo.tarotservice.infrastructure.persistence.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("arcana_type")
data class ArcanaTypeEntity(
    @Id
    val id: UUID? = null,
    @Column("name")
    val name: String,
)
