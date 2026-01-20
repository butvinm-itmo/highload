package com.github.butvinmitmo.userservice.domain.model

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID?,
    val username: String,
    val passwordHash: String,
    val role: Role,
    val createdAt: Instant?,
)
