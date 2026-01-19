package com.github.butvinmitmo.userservice.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("role")
data class Role(
    @Id
    val id: UUID? = null,
    @Column("name")
    val name: String,
)

enum class RoleType {
    USER,
    MEDIUM,
    ADMIN,
    ;

    companion object {
        val USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val MEDIUM_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000003")
        val ADMIN_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
    }
}
