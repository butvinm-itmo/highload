package com.itmo.tarot.dto.response

import com.itmo.tarot.entity.LayoutType
import java.time.LocalDateTime

data class SpreadListResponse(
    val id: Long,
    val question: String,
    val layoutType: LayoutType,
    val createdAt: LocalDateTime,
    val authorId: Long
)