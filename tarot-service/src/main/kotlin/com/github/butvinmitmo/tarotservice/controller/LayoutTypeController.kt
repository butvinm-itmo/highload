package com.github.butvinmitmo.tarotservice.controller

import com.github.butvinmitmo.shared.dto.LayoutTypeDto
import com.github.butvinmitmo.tarotservice.service.TarotService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v0.0.1/layout-types")
@Tag(name = "Layout Types", description = "Tarot spread layout type operations")
@Validated
class LayoutTypeController(
    private val tarotService: TarotService,
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
        @Min(0)
        page: Int,
        @Parameter(description = "Page size (max 50)", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(50)
        size: Int,
    ): ResponseEntity<List<LayoutTypeDto>> {
        val response = tarotService.getLayoutTypes(page, size)
        return ResponseEntity
            .ok()
            .header("X-Total-Count", response.totalElements.toString())
            .body(response.content)
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get layout type by ID",
        description = "Retrieves a specific layout type by its UUID",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Layout type found"),
            ApiResponse(responseCode = "404", description = "Layout type not found"),
        ],
    )
    fun getLayoutTypeById(
        @Parameter(description = "Layout Type ID", required = true)
        @PathVariable id: UUID,
    ): ResponseEntity<LayoutTypeDto> {
        val layoutType = tarotService.getLayoutTypeDtoById(id)
        return ResponseEntity.ok(layoutType)
    }
}
