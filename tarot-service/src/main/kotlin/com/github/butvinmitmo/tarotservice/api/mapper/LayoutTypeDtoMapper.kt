package com.github.butvinmitmo.tarotservice.api.mapper

import com.github.butvinmitmo.shared.dto.LayoutTypeDto
import com.github.butvinmitmo.tarotservice.domain.model.LayoutType
import org.springframework.stereotype.Component

@Component
class LayoutTypeDtoMapper {
    fun toDto(layoutType: LayoutType): LayoutTypeDto =
        LayoutTypeDto(
            id = layoutType.id,
            name = layoutType.name,
            cardsCount = layoutType.cardsCount,
        )
}
