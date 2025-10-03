package com.github.butvinmitmo.highload.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class SpreadDto(
    val id: UUID,
    val question: String?,
    val layoutType: LayoutTypeDto,
    val createdAt: Instant,
    val author: UserDto,
    val cards: List<SpreadCardDto> = emptyList(),
    val interpretations: List<InterpretationDto> = emptyList(),
)

data class SpreadSummaryDto(
    val id: UUID,
    val question: String?,
    val layoutTypeName: String,
    val createdAt: Instant,
    val authorUsername: String,
    val cardsCount: Int,
    val interpretationsCount: Int,
)

data class CreateSpreadRequest(
    @field:Size(max = 10000, message = "Question must not exceed 10000 characters")
    val question: String?,

    @field:NotNull(message = "Layout type ID is required")
    val layoutTypeId: UUID,

    @field:NotNull(message = "Author ID is required")
    val authorId: UUID,
)

enum class SpreadLayoutType(
    val cardsCount: Int,
) {
    ONE_CARD(1),
    THREE_CARDS(3),
    CROSS(5),
}
