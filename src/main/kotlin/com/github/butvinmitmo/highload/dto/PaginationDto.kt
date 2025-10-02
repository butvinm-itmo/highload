package com.github.butvinmitmo.highload.dto

import java.util.UUID

data class PageRequest(
    val page: Int = 0,
    val size: Int = 20,
)

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isFirst: Boolean,
    val isLast: Boolean,
)

data class ScrollRequest(
    val after: UUID? = null,
    val size: Int = 20,
)

data class ScrollResponse<T>(
    val content: List<T>,
    val hasNext: Boolean,
    val nextCursor: UUID? = null,
)