package com.github.butvinmitmo.highload.controller

import com.github.butvinmitmo.highload.dto.CardDto
import com.github.butvinmitmo.highload.service.TarotService
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v0.0.1/cards")
@Tag(name = "Cards", description = "Tarot card catalog operations")
@Validated
class CardController(
    private val tarotService: TarotService,
) {
    @GetMapping
    @Operation(
        summary = "Get paginated list of cards",
        description = "Retrieves a paginated list of all tarot cards in the catalog",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
        ],
    )
    fun getCards(
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0")
        @Min(0)
        page: Int,
        @Parameter(description = "Page size (max 50)", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(50)
        size: Int,
    ): ResponseEntity<List<CardDto>> {
        val response = tarotService.getCards(page, size)
        return ResponseEntity
            .ok()
            .header("X-Total-Count", response.totalElements.toString())
            .body(response.content)
    }
}
