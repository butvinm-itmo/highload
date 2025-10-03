package com.github.butvinmitmo.highload.mapper

import com.github.butvinmitmo.highload.dto.LayoutTypeDto
import com.github.butvinmitmo.highload.entity.LayoutType
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
