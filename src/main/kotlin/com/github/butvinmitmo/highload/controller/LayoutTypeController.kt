package com.github.butvinmitmo.highload.controller

import com.github.butvinmitmo.highload.dto.LayoutTypeDto
import com.github.butvinmitmo.highload.service.LayoutTypeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v0.0.1/layout-types")
@Tag(name = "Layout Types", description = "Tarot spread layout type operations")
class LayoutTypeController(
    private val layoutTypeService: LayoutTypeService,
) {
    @GetMapping
    @Operation(
        summary = "Get paginated list of layout types",
        description = "Retrieves a paginated list of all available tarot spread layout types",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Layout types retrieved successfully"),
        ],
    )
    fun getLayoutTypes(
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0")
        page: Int,
        @Parameter(description = "Page size", example = "20")
        @RequestParam(defaultValue = "20")
        size: Int,
    ): List<LayoutTypeDto> = layoutTypeService.getLayoutTypes(page, size)
}
