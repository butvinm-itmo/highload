package com.github.butvinmitmo.highload.mapper

import com.github.butvinmitmo.highload.dto.ArcanaTypeDto
import com.github.butvinmitmo.highload.entity.ArcanaType
import org.springframework.stereotype.Component

@Component
class ArcanaTypeMapper {
    fun toDto(arcanaType: ArcanaType): ArcanaTypeDto =
        ArcanaTypeDto(
            id = arcanaType.id,
            name = arcanaType.name,
        )
}
