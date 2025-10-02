package com.github.butvinmitmo.highload.dto

import java.time.Instant
import java.util.UUID

data class UserDto(
    val id: UUID,
    val username: String,
    val createdAt: Instant,
)

data class CreateUserRequest(
    val username: String,
)

data class UpdateUserRequest(
    val username: String,
)
