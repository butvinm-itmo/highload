package com.github.butvinmitmo.tarotservice.api.controller

import com.github.butvinmitmo.shared.dto.LayoutTypeDto
import com.github.butvinmitmo.tarotservice.api.mapper.LayoutTypeDtoMapper
import com.github.butvinmitmo.tarotservice.application.service.TarotService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/v0.0.1/layout-types")
@Tag(name = "Layout Types", description = "Tarot spread layout type operations")
@Validated
class LayoutTypeController(
    private val tarotService: TarotService,
    private val layoutTypeDtoMapper: LayoutTypeDtoMapper,
) {
    @GetMapping
    @Operation(
        summary = "Get paginated list of layout types",
        description = "Retrieves a paginated list of all available tarot spread layout types",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Layout types retrieved successfully",
                headers = [
                    Header(
                        name = "X-Total-Count",
                        description = "Total number of layout types",
                        schema = Schema(type = "integer"),
                    ),
                ],
                content = [Content(array = ArraySchema(schema = Schema(implementation = LayoutTypeDto::class)))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Missing or invalid authentication",
                content = [
                    Content(
                        schema = Schema(implementation = com.github.butvinmitmo.shared.dto.ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun getLayoutTypes(
        @Parameter(description = "User ID from JWT", required = true)
        @RequestHeader("X-User-Id")
        userId: UUID,
        @Parameter(description = "User role from JWT", required = true)
        @RequestHeader("X-User-Role")
        role: String,
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0")
        @Min(0)
        page: Int,
        @Parameter(description = "Page size (max 50)", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(50)
        size: Int,
    ): Mono<ResponseEntity<List<LayoutTypeDto>>> =
        tarotService
            .getLayoutTypes(page, size)
            .map { result ->
                ResponseEntity
                    .ok()
                    .header("X-Total-Count", result.totalElements.toString())
                    .body(result.content.map { layoutTypeDtoMapper.toDto(it) })
            }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get layout type by ID",
        description = "Retrieves a specific layout type by its UUID",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Layout type found",
                content = [Content(schema = Schema(implementation = LayoutTypeDto::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Layout type not found",
                content = [
                    Content(
                        schema = Schema(implementation = com.github.butvinmitmo.shared.dto.ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Missing or invalid authentication",
                content = [
                    Content(
                        schema = Schema(implementation = com.github.butvinmitmo.shared.dto.ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun getLayoutTypeById(
        @Parameter(description = "User ID from JWT", required = true)
        @RequestHeader("X-User-Id")
        userId: UUID,
        @Parameter(description = "User role from JWT", required = true)
        @RequestHeader("X-User-Role")
        role: String,
        @Parameter(description = "Layout Type ID", required = true)
        @PathVariable id: UUID,
    ): Mono<ResponseEntity<LayoutTypeDto>> =
        tarotService
            .getLayoutTypeById(id)
            .map { layoutType ->
                ResponseEntity.ok(layoutTypeDtoMapper.toDto(layoutType))
            }
}
