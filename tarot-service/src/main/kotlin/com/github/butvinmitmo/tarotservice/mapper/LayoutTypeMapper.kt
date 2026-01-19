package com.github.butvinmitmo.tarotservice.mapper

import com.github.butvinmitmo.shared.dto.LayoutTypeDto
import com.github.butvinmitmo.tarotservice.entity.LayoutType
import org.springframework.stereotype.Component

@Component
class LayoutTypeMapper {
    fun toDto(layoutType: LayoutType): LayoutTypeDto =
        LayoutTypeDto(
            id = layoutType.id!!,
            name = layoutType.name,
            cardsCount = layoutType.cardsCount,
        )
}
