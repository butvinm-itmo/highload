package com.github.butvinmitmo.divinationservice.mapper

import com.github.butvinmitmo.divinationservice.entity.Interpretation
import com.github.butvinmitmo.shared.client.UserServiceClient
import com.github.butvinmitmo.shared.dto.InterpretationDto
import org.springframework.stereotype.Component

@Component
class InterpretationMapper(
    private val userServiceClient: UserServiceClient,
) {
    fun toDto(interpretation: Interpretation): InterpretationDto {
        val author = userServiceClient.getUserById(interpretation.authorId).body!!
        return InterpretationDto(
            id = interpretation.id,
            text = interpretation.text,
            author = author,
            spreadId = interpretation.spread.id,
            createdAt = interpretation.createdAt,
        )
    }
}
