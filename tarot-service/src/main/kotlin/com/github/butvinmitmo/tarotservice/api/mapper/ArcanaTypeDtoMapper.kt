package com.github.butvinmitmo.tarotservice.api.mapper

import com.github.butvinmitmo.shared.dto.ArcanaTypeDto
import com.github.butvinmitmo.tarotservice.domain.model.ArcanaType
import org.springframework.stereotype.Component

@Component
class ArcanaTypeDtoMapper {
    fun toDto(arcanaType: ArcanaType): ArcanaTypeDto =
        ArcanaTypeDto(
            id = arcanaType.id,
            name = arcanaType.name,
        )
}
