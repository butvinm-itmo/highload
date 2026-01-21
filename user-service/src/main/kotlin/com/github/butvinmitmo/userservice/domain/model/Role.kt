package com.github.butvinmitmo.userservice.domain.model

import java.util.UUID

data class Role(
    val id: UUID,
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
