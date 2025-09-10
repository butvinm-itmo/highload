package com.itmo.tarot.dto.response

import java.time.LocalDateTime

data class InterpretationResponse(
    val id: Long,
    val text: String,
    val createdAt: LocalDateTime,
    val authorId: Long
)