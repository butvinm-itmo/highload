package com.github.butvinmitmo.divinationservice.mapper

import com.github.butvinmitmo.divinationservice.client.UserClient
import com.github.butvinmitmo.divinationservice.entity.Interpretation
import com.github.butvinmitmo.shared.dto.InterpretationDto
import org.springframework.stereotype.Component

@Component
class InterpretationMapper(
    private val userClient: UserClient,
) {
    fun toDto(interpretation: Interpretation): InterpretationDto {
        val author = userClient.getUserById(interpretation.authorId).body!!
        return InterpretationDto(
            id = interpretation.id,
            text = interpretation.text,
            author = author,
            spreadId = interpretation.spread.id,
            createdAt = interpretation.createdAt,
        )
    }
}
