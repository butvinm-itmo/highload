package com.github.butvinmitmo.highload.controller

import com.github.butvinmitmo.highload.dto.CreateInterpretationRequest
import com.github.butvinmitmo.highload.dto.DeleteRequest
import com.github.butvinmitmo.highload.dto.InterpretationDto
import com.github.butvinmitmo.highload.dto.UpdateInterpretationRequest
import com.github.butvinmitmo.highload.service.InterpretationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v0.0.1/spreads/{spreadId}/interpretations")
@Tag(name = "Interpretations", description = "Manage interpretations for tarot spreads")
class InterpretationController(
    private val interpretationService: InterpretationService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Add interpretation to a spread",
        description = "Creates a new interpretation for a specific spread. " +
            "Each user can have only one interpretation per spread (unique constraint on author_id + spread_id).",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Interpretation added successfully"),
            ApiResponse(responseCode = "404", description = "Spread or author not found"),
            ApiResponse(responseCode = "409", description = "User already has an interpretation for this spread"),
            ApiResponse(responseCode = "400", description = "Invalid request data"),
        ],
    )
    fun addInterpretation(
        @Parameter(description = "Spread ID to add interpretation to", required = true)
        @PathVariable
        spreadId: UUID,
        @RequestBody request: CreateInterpretationRequest,
    ): InterpretationDto = interpretationService.addInterpretation(spreadId, request)

    @PutMapping("/{id}")
    @Operation(
        summary = "Update interpretation",
        description = "Updates an existing interpretation. Only the interpretation author can update it.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Interpretation updated successfully"),
            ApiResponse(responseCode = "404", description = "Spread or interpretation not found"),
            ApiResponse(responseCode = "403", description = "User is not the author of the interpretation"),
            ApiResponse(responseCode = "400", description = "Invalid request data"),
        ],
    )
    fun updateInterpretation(
        @Parameter(description = "Spread ID containing the interpretation", required = true)
        @PathVariable
        spreadId: UUID,
        @Parameter(description = "Interpretation ID to update", required = true)
        @PathVariable
        id: UUID,
        @RequestBody request: UpdateInterpretationRequest,
    ): InterpretationDto {
        // TODO: Get userId from authentication context
        val userId = UUID.randomUUID() // Placeholder for now
        return interpretationService.updateInterpretation(spreadId, id, userId, request)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Delete interpretation",
        description = "Deletes an interpretation from a spread. Only the interpretation author can delete it.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Interpretation deleted successfully"),
            ApiResponse(responseCode = "404", description = "Spread or interpretation not found"),
            ApiResponse(responseCode = "403", description = "User is not the author of the interpretation"),
        ],
    )
    fun deleteInterpretation(
        @Parameter(description = "Spread ID containing the interpretation", required = true)
        @PathVariable
        spreadId: UUID,
        @Parameter(description = "Interpretation ID to delete", required = true)
        @PathVariable
        id: UUID,
        @RequestBody request: DeleteRequest,
    ) {
        interpretationService.deleteInterpretation(spreadId, id, request.userId)
    }
}
