package com.github.butvinmitmo.tarotservice.infrastructure.persistence.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("card")
data class CardEntity(
    @Id
    val id: UUID? = null,
    @Column("name")
    val name: String,
    @Column("arcana_type_id")
    val arcanaTypeId: UUID,
)
