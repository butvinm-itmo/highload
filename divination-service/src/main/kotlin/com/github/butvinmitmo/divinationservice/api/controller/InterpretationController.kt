package com.github.butvinmitmo.divinationservice.api.controller

import com.github.butvinmitmo.divinationservice.application.service.DivinationService
import com.github.butvinmitmo.shared.dto.CreateInterpretationRequest
import com.github.butvinmitmo.shared.dto.CreateInterpretationResponse
import com.github.butvinmitmo.shared.dto.InterpretationDto
import com.github.butvinmitmo.shared.dto.UpdateInterpretationRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/v0.0.1/spreads/{spreadId}/interpretations")
@Tag(name = "Interpretations", description = "Manage interpretations for tarot spreads")
@Validated
class InterpretationController(
    private val divinationService: DivinationService,
) {
    @GetMapping
    @Operation(
        summary = "Get all interpretations for a spread",
        description =
            "Retrieves paginated interpretations for the specified spread, ordered by creation date (newest first)",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Interpretations retrieved successfully",
                headers = [
                    Header(
                        name = "X-Total-Count",
                        description = "Total number of interpretations",
                        schema = Schema(type = "integer", implementation = Int::class),
                    ),
                ],
                content = [Content(array = ArraySchema(schema = Schema(implementation = InterpretationDto::class)))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Spread not found",
                content = [
                    Content(
                        schema = Schema(implementation = com.github.butvinmitmo.shared.dto.ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun getInterpretations(
        @Parameter(description = "Spread ID to get interpretations for", required = true)
        @PathVariable
        spreadId: UUID,
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0")
        @Min(0)
        page: Int,
        @Parameter(description = "Page size (max 50)", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(50)
        size: Int,
    ): Mono<ResponseEntity<List<InterpretationDto>>> =
        divinationService
            .getInterpretations(spreadId, page, size)
            .map { response ->
                ResponseEntity
                    .ok()
                    .header("X-Total-Count", response.totalElements.toString())
                    .body(response.content)
            }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get interpretation details",
        description = "Retrieves a specific interpretation by its ID",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Interpretation found",
                content = [Content(schema = Schema(implementation = InterpretationDto::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Interpretation or spread not found",
                content = [
                    Content(
                        schema = Schema(implementation = com.github.butvinmitmo.shared.dto.ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun getInterpretation(
        @Parameter(description = "Spread ID containing the interpretation", required = true)
        @PathVariable
        spreadId: UUID,
        @Parameter(description = "Interpretation ID", required = true)
        @PathVariable
        id: UUID,
    ): Mono<ResponseEntity<InterpretationDto>> =
        divinationService
            .getInterpretation(spreadId, id)
            .map { interpretation -> ResponseEntity.ok(interpretation) }

    @PostMapping
    @PreAuthorize("hasAnyRole('MEDIUM', 'ADMIN')")
    @Operation(
        summary = "Add interpretation to a spread",
        description =
            "Creates a new interpretation. Each user can have only one interpretation per spread. " +
                "ID and timestamp are generated by PostgreSQL. Returns only the generated ID. " +
                "Only MEDIUM and ADMIN roles can create interpretations.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Interpretation added successfully, returns generated ID",
                content = [Content(schema = Schema(implementation = CreateInterpretationResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "User is not MEDIUM or ADMIN",
                content = [
                    Content(
                        schema = Schema(implementation = com.github.butvinmitmo.shared.dto.ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Spread or author not found",
                content = [
                    Content(
                        schema = Schema(implementation = com.github.butvinmitmo.shared.dto.ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "User already has an interpretation for this spread",
                content = [
                    Content(
                        schema = Schema(implementation = com.github.butvinmitmo.shared.dto.ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request data",
                content = [
                    Content(
                        schema =
                            Schema(
                                implementation = com.github.butvinmitmo.shared.dto.ValidationErrorResponse::class,
                            ),
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
    fun addInterpretation(
        @Parameter(description = "Spread ID to add interpretation to", required = true)
        @PathVariable
        spreadId: UUID,
        @Valid @RequestBody request: CreateInterpretationRequest,
    ): Mono<ResponseEntity<CreateInterpretationResponse>> =
        divinationService
            .addInterpretation(spreadId, request.text)
            .map { result ->
                ResponseEntity.status(HttpStatus.CREATED).body(CreateInterpretationResponse(id = result.id))
            }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update interpretation",
        description = "Updates an existing interpretation. Only the interpretation author can update it.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Interpretation updated successfully",
                content = [Content(schema = Schema(implementation = InterpretationDto::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Spread or interpretation not found",
                content = [
                    Content(
                        schema = Schema(implementation = com.github.butvinmitmo.shared.dto.ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "User is not the author of the interpretation",
                content = [
                    Content(
                        schema = Schema(implementation = com.github.butvinmitmo.shared.dto.ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request data",
                content = [
                    Content(
                        schema =
                            Schema(
                                implementation = com.github.butvinmitmo.shared.dto.ValidationErrorResponse::class,
                            ),
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
    fun updateInterpretation(
        @Parameter(description = "Spread ID containing the interpretation", required = true)
        @PathVariable
        spreadId: UUID,
        @Parameter(description = "Interpretation ID to update", required = true)
        @PathVariable
        id: UUID,
        @Valid @RequestBody request: UpdateInterpretationRequest,
    ): Mono<ResponseEntity<InterpretationDto>> =
        divinationService
            .updateInterpretation(spreadId, id, request.text)
            .map { updatedInterpretation -> ResponseEntity.ok(updatedInterpretation) }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete interpretation",
        description = "Deletes an interpretation from a spread. Only the interpretation author can delete it.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Interpretation deleted successfully"),
            ApiResponse(
                responseCode = "404",
                description = "Spread or interpretation not found",
                content = [
                    Content(
                        schema = Schema(implementation = com.github.butvinmitmo.shared.dto.ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "User is not the author of the interpretation",
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
    fun deleteInterpretation(
        @Parameter(description = "Spread ID containing the interpretation", required = true)
        @PathVariable
        spreadId: UUID,
        @Parameter(description = "Interpretation ID to delete", required = true)
        @PathVariable
        id: UUID,
    ): Mono<ResponseEntity<Void>> =
        divinationService
            .deleteInterpretation(spreadId, id)
            .then(Mono.just(ResponseEntity.noContent().build()))
}
