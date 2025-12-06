package com.github.butvinmitmo.shared.dto

import java.util.UUID

data class ScrollResponse<T>(
    val items: List<T>,
    val nextCursor: UUID?,
)
