package com.github.butvinmitmo.divinationservice.infrastructure.messaging.mapper

import com.github.butvinmitmo.divinationservice.domain.model.Interpretation
import com.github.butvinmitmo.shared.dto.events.InterpretationEventData
import org.springframework.stereotype.Component

@Component
class InterpretationEventDataMapper {
    fun toEventData(interpretation: Interpretation): InterpretationEventData =
        InterpretationEventData(
            id = interpretation.id!!,
            text = interpretation.text,
            authorId = interpretation.authorId,
            spreadId = interpretation.spreadId,
            createdAt = interpretation.createdAt!!,
        )
}
