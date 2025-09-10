package com.itmo.tarot.controller

import com.itmo.tarot.dto.request.CreateSpreadRequest
import com.itmo.tarot.dto.request.DeleteSpreadRequest
import com.itmo.tarot.dto.response.SpreadListResponse
import com.itmo.tarot.dto.response.SpreadResponse
import com.itmo.tarot.service.SpreadService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/spreads")
@Tag(name = "Spreads", description = "Tarot spread management")
@Validated
class SpreadController(
    private val spreadService: SpreadService
) {
    
    @PostMapping
    @Operation(summary = "Create a new tarot spread")
    fun createSpread(@Valid @RequestBody request: CreateSpreadRequest): ResponseEntity<SpreadResponse> {
        val spread = spreadService.createSpread(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(spread)
    }
    
    @GetMapping
    @Operation(summary = "Get paginated list of all spreads with total count in header")
    fun getSpreads(
        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") 
        @Min(0) page: Int,
        
        @Parameter(description = "Page size (max 50)")
        @RequestParam(defaultValue = "20") 
        @Min(1) @Max(50) size: Int
    ): ResponseEntity<List<SpreadListResponse>> {
        val pageable = PageRequest.of(page, size)
        val spreadsPage: Page<SpreadListResponse> = spreadService.findAll(pageable)
        
        val headers = HttpHeaders().apply {
            set("X-Total-Count", spreadsPage.totalElements.toString())
        }
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(spreadsPage.content)
    }
    
    @GetMapping("/scroll")
    @Operation(summary = "Get spreads for infinite scroll without total count")
    fun getSpreadsForInfiniteScroll(
        @Parameter(description = "ID to start after (for pagination)")
        @RequestParam(required = false) after: Long?,
        
        @Parameter(description = "Number of records to fetch")
        @RequestParam(defaultValue = "20") 
        @Min(1) @Max(50) size: Int
    ): ResponseEntity<List<SpreadListResponse>> {
        val pageable = PageRequest.of(0, size)
        val spreads = spreadService.findAllForInfiniteScroll(after, pageable)
        
        return ResponseEntity.ok(spreads)
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information about a specific spread")
    fun getSpread(
        @Parameter(description = "Spread ID")
        @PathVariable id: Long
    ): ResponseEntity<SpreadResponse> {
        val spread = spreadService.findById(id)
        return ResponseEntity.ok(spread)
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a spread")
    fun deleteSpread(
        @Parameter(description = "Spread ID")
        @PathVariable id: Long,
        
        @Valid @RequestBody request: DeleteSpreadRequest
    ): ResponseEntity<Void> {
        spreadService.deleteSpread(id, request.userId)
        return ResponseEntity.noContent().build()
    }
}