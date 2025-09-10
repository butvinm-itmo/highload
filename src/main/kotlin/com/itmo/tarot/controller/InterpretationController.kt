package com.itmo.tarot.controller

import com.itmo.tarot.dto.request.CreateInterpretationRequest
import com.itmo.tarot.dto.request.DeleteInterpretationRequest
import com.itmo.tarot.dto.request.UpdateInterpretationRequest
import com.itmo.tarot.dto.response.InterpretationResponse
import com.itmo.tarot.service.InterpretationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/spreads/{spreadId}/interpretations")
@Tag(name = "Interpretations", description = "Tarot spread interpretation management")
@Validated
class InterpretationController(
    private val interpretationService: InterpretationService
) {
    
    @PostMapping
    @Operation(summary = "Add a new interpretation to a spread")
    fun createInterpretation(
        @Parameter(description = "Spread ID")
        @PathVariable spreadId: Long,
        
        @Valid @RequestBody request: CreateInterpretationRequest
    ): ResponseEntity<InterpretationResponse> {
        val interpretation = interpretationService.createInterpretation(spreadId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(interpretation)
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing interpretation")
    fun updateInterpretation(
        @Parameter(description = "Spread ID")
        @PathVariable spreadId: Long,
        
        @Parameter(description = "Interpretation ID")
        @PathVariable id: Long,
        
        @Valid @RequestBody request: UpdateInterpretationRequest
    ): ResponseEntity<InterpretationResponse> {
        val interpretation = interpretationService.updateInterpretation(spreadId, id, request)
        return ResponseEntity.ok(interpretation)
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an interpretation")
    fun deleteInterpretation(
        @Parameter(description = "Spread ID")
        @PathVariable spreadId: Long,
        
        @Parameter(description = "Interpretation ID")
        @PathVariable id: Long,
        
        @Valid @RequestBody request: DeleteInterpretationRequest
    ): ResponseEntity<Void> {
        interpretationService.deleteInterpretation(spreadId, id, request.userId)
        return ResponseEntity.noContent().build()
    }
    
    @GetMapping
    @Operation(summary = "Get all interpretations for a spread")
    fun getInterpretations(
        @Parameter(description = "Spread ID")
        @PathVariable spreadId: Long
    ): ResponseEntity<List<InterpretationResponse>> {
        val interpretations = interpretationService.findBySpreadId(spreadId)
        return ResponseEntity.ok(interpretations)
    }
}