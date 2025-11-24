package com.github.butvinmitmo.highload.unit.service

import com.github.butvinmitmo.highload.dto.LayoutTypeDto
import com.github.butvinmitmo.highload.entity.LayoutType
import com.github.butvinmitmo.highload.mapper.LayoutTypeMapper
import com.github.butvinmitmo.highload.repository.LayoutTypeRepository
import com.github.butvinmitmo.highload.service.LayoutTypeService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class LayoutTypeServiceTest {
    @Mock
    private lateinit var layoutTypeRepository: LayoutTypeRepository

    @Mock
    private lateinit var layoutTypeMapper: LayoutTypeMapper

    private lateinit var layoutTypeService: LayoutTypeService

    private val layoutTypeId1 = UUID.randomUUID()
    private val layoutTypeId2 = UUID.randomUUID()
    private val layoutTypeId3 = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        layoutTypeService = LayoutTypeService(layoutTypeRepository, layoutTypeMapper)
    }

    private fun createLayoutType(
        id: UUID,
        name: String,
        cardsCount: Int,
    ): LayoutType {
        val layoutType = LayoutType(name = name, cardsCount = cardsCount)
        layoutType.id = id
        return layoutType
    }

    @Test
    fun `getLayoutTypes should return paginated list of layout type DTOs`() {
        // Given
        val layoutTypes =
            listOf(
                createLayoutType(layoutTypeId1, "ONE_CARD", 1),
                createLayoutType(layoutTypeId2, "THREE_CARDS", 3),
                createLayoutType(layoutTypeId3, "CROSS", 5),
            )

        val page = PageImpl(layoutTypes, PageRequest.of(0, 3), 3)
        whenever(layoutTypeRepository.findAll(any<PageRequest>())).thenReturn(page)

        whenever(layoutTypeMapper.toDto(layoutTypes[0])).thenReturn(LayoutTypeDto(layoutTypeId1, "ONE_CARD", 1))
        whenever(layoutTypeMapper.toDto(layoutTypes[1])).thenReturn(LayoutTypeDto(layoutTypeId2, "THREE_CARDS", 3))
        whenever(layoutTypeMapper.toDto(layoutTypes[2])).thenReturn(LayoutTypeDto(layoutTypeId3, "CROSS", 5))

        // When
        val result = layoutTypeService.getLayoutTypes(0, 3)

        // Then
        assertNotNull(result)
        assertEquals(3, result.content.size)
        assertEquals("ONE_CARD", result.content[0].name)
        assertEquals(1, result.content[0].cardsCount)
        assertEquals("THREE_CARDS", result.content[1].name)
        assertEquals(3, result.content[1].cardsCount)
        assertEquals("CROSS", result.content[2].name)
        assertEquals(5, result.content[2].cardsCount)
    }

    @Test
    fun `getLayoutTypes should return empty list when no layout types exist`() {
        // Given
        val page = PageImpl<LayoutType>(emptyList(), PageRequest.of(0, 20), 0)
        whenever(layoutTypeRepository.findAll(any<PageRequest>())).thenReturn(page)

        // When
        val result = layoutTypeService.getLayoutTypes(0, 20)

        // Then
        assertNotNull(result)
        assertEquals(0, result.content.size)
    }

    @Test
    fun `getLayoutTypes should return correct page with custom size`() {
        // Given
        val layoutTypes =
            listOf(
                createLayoutType(layoutTypeId1, "ONE_CARD", 1),
                createLayoutType(layoutTypeId2, "THREE_CARDS", 3),
            )

        val page = PageImpl(layoutTypes, PageRequest.of(0, 2), 3)
        whenever(layoutTypeRepository.findAll(any<PageRequest>())).thenReturn(page)

        whenever(layoutTypeMapper.toDto(layoutTypes[0])).thenReturn(LayoutTypeDto(layoutTypeId1, "ONE_CARD", 1))
        whenever(layoutTypeMapper.toDto(layoutTypes[1])).thenReturn(LayoutTypeDto(layoutTypeId2, "THREE_CARDS", 3))

        // When
        val result = layoutTypeService.getLayoutTypes(0, 2)

        // Then
        assertNotNull(result)
        assertEquals(2, result.content.size)
        assertEquals("ONE_CARD", result.content[0].name)
        assertEquals("THREE_CARDS", result.content[1].name)
    }

    @Test
    fun `getLayoutTypes should handle second page correctly`() {
        // Given
        val layoutType = createLayoutType(layoutTypeId3, "CROSS", 5)

        val page = PageImpl(listOf(layoutType), PageRequest.of(1, 2), 3)
        whenever(layoutTypeRepository.findAll(any<PageRequest>())).thenReturn(page)

        whenever(layoutTypeMapper.toDto(layoutType)).thenReturn(LayoutTypeDto(layoutTypeId3, "CROSS", 5))

        // When
        val result = layoutTypeService.getLayoutTypes(1, 2)

        // Then
        assertNotNull(result)
        assertEquals(1, result.content.size)
        assertEquals("CROSS", result.content[0].name)
        assertEquals(5, result.content[0].cardsCount)
    }

    @Test
    fun `getLayoutTypes should return single layout type`() {
        // Given
        val layoutType = createLayoutType(layoutTypeId1, "ONE_CARD", 1)

        val page = PageImpl(listOf(layoutType), PageRequest.of(0, 1), 3)
        whenever(layoutTypeRepository.findAll(any<PageRequest>())).thenReturn(page)

        whenever(layoutTypeMapper.toDto(layoutType)).thenReturn(LayoutTypeDto(layoutTypeId1, "ONE_CARD", 1))

        // When
        val result = layoutTypeService.getLayoutTypes(0, 1)

        // Then
        assertNotNull(result)
        assertEquals(1, result.content.size)
        assertEquals("ONE_CARD", result.content[0].name)
        assertEquals(1, result.content[0].cardsCount)
    }
}
