package com.github.butvinmitmo.tarotservice.mapper

import com.github.butvinmitmo.shared.dto.ArcanaTypeDto
import com.github.butvinmitmo.tarotservice.entity.ArcanaType
import org.springframework.stereotype.Component

@Component
class ArcanaTypeMapper {
    fun toDto(arcanaType: ArcanaType): ArcanaTypeDto =
        ArcanaTypeDto(
            id = arcanaType.id,
            name = arcanaType.name,
        )
}
