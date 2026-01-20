package com.github.butvinmitmo.userservice.infrastructure.persistence.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("role")
data class RoleEntity(
    @Id
    val id: UUID? = null,
    @Column("name")
    val name: String,
)
