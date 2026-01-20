package com.github.butvinmitmo.divinationservice.infrastructure.messaging.mapper

import com.github.butvinmitmo.divinationservice.domain.model.Spread
import com.github.butvinmitmo.shared.dto.events.SpreadEventData
import org.springframework.stereotype.Component

@Component
class SpreadEventDataMapper {
    fun toEventData(spread: Spread): SpreadEventData =
        SpreadEventData(
            id = spread.id!!,
            question = spread.question,
            layoutTypeId = spread.layoutTypeId,
            authorId = spread.authorId,
            createdAt = spread.createdAt!!,
        )
}
