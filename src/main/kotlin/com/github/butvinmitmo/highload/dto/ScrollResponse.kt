package com.github.butvinmitmo.highload.dto

import java.util.UUID

data class ScrollResponse<T>(
    val items: List<T>,
    val nextCursor: UUID?,
)
