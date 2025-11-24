package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.dto.LayoutTypeDto
import com.github.butvinmitmo.highload.dto.PageResponse
import com.github.butvinmitmo.highload.mapper.LayoutTypeMapper
import com.github.butvinmitmo.highload.repository.LayoutTypeRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class LayoutTypeService(
    private val layoutTypeRepository: LayoutTypeRepository,
    private val layoutTypeMapper: LayoutTypeMapper,
) {
    fun getLayoutTypes(
        page: Int,
        size: Int,
    ): PageResponse<LayoutTypeDto> {
        val pageable = PageRequest.of(page, size)
        val layoutTypesPage = layoutTypeRepository.findAll(pageable)
        return PageResponse(
            content = layoutTypesPage.content.map { layoutTypeMapper.toDto(it) },
            page = layoutTypesPage.number,
            size = layoutTypesPage.size,
            totalElements = layoutTypesPage.totalElements,
            totalPages = layoutTypesPage.totalPages,
            isFirst = layoutTypesPage.isFirst,
            isLast = layoutTypesPage.isLast,
        )
    }
}
