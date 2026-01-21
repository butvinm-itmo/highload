package com.github.butvinmitmo.shared.dto.events

import java.time.Instant
import java.util.UUID

data class UserEventData(
    val id: UUID,
    val username: String,
    val role: String,
    val createdAt: Instant,
)
