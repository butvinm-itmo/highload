package com.github.butvinmitmo.highload.controller

import com.github.butvinmitmo.highload.dto.CreateSpreadRequest
import com.github.butvinmitmo.highload.dto.DeleteRequest
import com.github.butvinmitmo.highload.dto.SpreadDto
import com.github.butvinmitmo.highload.dto.SpreadSummaryDto
import com.github.butvinmitmo.highload.service.SpreadService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v0.0.1/spreads")
@Tag(name = "Spreads", description = "Tarot spread management and viewing")
class SpreadController(
    private val spreadService: SpreadService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a new spread",
        description = "Creates a new tarot spread with randomly selected cards based on the layout type. " +
            "Cards are automatically assigned positions and may be reversed.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Spread created successfully"),
            ApiResponse(responseCode = "404", description = "Author or layout type not found"),
            ApiResponse(responseCode = "400", description = "Invalid request data"),
        ],
    )
    fun createSpread(
        @RequestBody request: CreateSpreadRequest,
    ): SpreadDto = spreadService.createSpread(request)

    @GetMapping
    @Operation(
        summary = "Get paginated spreads",
        description = "Retrieves a paginated list of all spreads sorted by creation date (newest first). " +
            "Returns total count in X-Total-Count header.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Spreads retrieved successfully",
                headers = [Header(name = "X-Total-Count", description = "Total number of spreads", schema = Schema(type = "integer"))],
                content = [Content(array = ArraySchema(schema = Schema(implementation = SpreadSummaryDto::class)))],
            ),
        ],
    )
    fun getSpreads(
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0")
        page: Int,
        @Parameter(description = "Page size", example = "20")
        @RequestParam(defaultValue = "20")
        size: Int,
    ): ResponseEntity<List<SpreadSummaryDto>> {
        val response = spreadService.getSpreads(page, size)
        return ResponseEntity
            .ok()
            .header("X-Total-Count", response.totalElements.toString())
            .body(response.content)
    }

    @GetMapping("/scroll")
    @Operation(
        summary = "Get spreads with infinite scroll",
        description = "Retrieves spreads for infinite scrolling using cursor-based pagination. " +
            "Provide the ID of the last spread from previous request to get the next batch.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Spreads retrieved successfully"),
        ],
    )
    fun getSpreadsByScroll(
        @Parameter(description = "ID of the last spread from previous request (cursor)", required = false)
        @RequestParam(required = false)
        after: UUID?,
        @Parameter(description = "Number of spreads to retrieve", example = "20")
        @RequestParam(defaultValue = "20")
        size: Int,
    ): List<SpreadSummaryDto> = spreadService.getSpreadsByScroll(after, size)

    @GetMapping("/{id}")
    @Operation(
        summary = "Get spread details",
        description = "Retrieves complete details of a specific spread including all cards and interpretations",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Spread found"),
            ApiResponse(responseCode = "404", description = "Spread not found"),
        ],
    )
    fun getSpread(
        @Parameter(description = "Spread ID", required = true)
        @PathVariable
        id: UUID,
    ): SpreadDto = spreadService.getSpread(id)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Delete a spread",
        description = "Deletes a spread and all its associated data (cards, interpretations). " +
            "Only the spread author can delete it.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Spread deleted successfully"),
            ApiResponse(responseCode = "404", description = "Spread not found"),
            ApiResponse(responseCode = "403", description = "User is not the author of the spread"),
        ],
    )
    fun deleteSpread(
        @Parameter(description = "Spread ID", required = true)
        @PathVariable
        id: UUID,
        @RequestBody request: DeleteRequest,
    ) {
        spreadService.deleteSpread(id, request.userId)
    }
}
