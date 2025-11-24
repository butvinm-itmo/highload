package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.dto.LayoutTypeDto
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
    ): List<LayoutTypeDto> {
        val pageable = PageRequest.of(page, size)
        return layoutTypeRepository.findAll(pageable).content.map { layoutTypeMapper.toDto(it) }
    }
}
