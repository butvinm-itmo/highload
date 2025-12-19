package com.github.butvinmitmo.userservice.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "role")
class Role(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 50)
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
