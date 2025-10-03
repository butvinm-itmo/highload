package com.github.butvinmitmo.highload.mapper

import com.github.butvinmitmo.highload.dto.InterpretationDto
import com.github.butvinmitmo.highload.dto.InterpretationSummaryDto
import com.github.butvinmitmo.highload.dto.UserDto
import com.github.butvinmitmo.highload.entity.Interpretation
import org.springframework.stereotype.Component

@Component
class InterpretationMapper {
    fun toDto(interpretation: Interpretation): InterpretationDto =
        InterpretationDto(
            id = interpretation.id!!,
            text = interpretation.text,
            author =
                UserDto(
                    id = interpretation.author.id!!,
                    username = interpretation.author.username,
                    createdAt = interpretation.author.createdAt,
                ),
            spreadId = interpretation.spread.id!!,
            createdAt = interpretation.createdAt,
        )

    fun toSummaryDto(interpretation: Interpretation): InterpretationSummaryDto =
        InterpretationSummaryDto(
            id = interpretation.id!!,
            text = interpretation.text,
            createdAt = interpretation.createdAt,
            authorUsername = interpretation.author.username,
        )
}
