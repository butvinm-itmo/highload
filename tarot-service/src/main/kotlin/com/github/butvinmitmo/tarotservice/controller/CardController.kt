package com.github.butvinmitmo.tarotservice.controller

import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.tarotservice.service.TarotService
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
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

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
            ApiResponse(
                responseCode = "200",
                description = "Cards retrieved successfully",
                headers = [
                    Header(
                        name = "X-Total-Count",
                        description = "Total number of cards",
                        schema = Schema(type = "integer"),
                    ),
                ],
                content = [Content(array = ArraySchema(schema = Schema(implementation = CardDto::class)))],
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
    fun getCards(
        @Parameter(description = "User ID from JWT", required = true)
        @RequestHeader("X-User-Id")
        userId: UUID,
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0")
        @Min(0)
        page: Int,
        @Parameter(description = "Page size (max 50)", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(50)
        size: Int,
    ): Mono<ResponseEntity<List<CardDto>>> =
        tarotService
            .getCards(page, size)
            .map { response ->
                ResponseEntity
                    .ok()
                    .header("X-Total-Count", response.totalElements.toString())
                    .body(response.content)
            }

    @GetMapping("/random")
    @Operation(
        summary = "Get random cards",
        description = "Retrieves a specified number of random tarot cards",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Random cards retrieved successfully",
                content = [Content(array = ArraySchema(schema = Schema(implementation = CardDto::class)))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid count parameter",
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
    fun getRandomCards(
        @Parameter(description = "User ID from JWT", required = true)
        @RequestHeader("X-User-Id")
        userId: UUID,
        @Parameter(description = "Number of random cards to retrieve (1-78)", example = "3")
        @RequestParam
        @Min(1)
        @Max(78)
        count: Int,
    ): Mono<ResponseEntity<List<CardDto>>> =
        tarotService
            .getRandomCardDtos(count)
            .map { cards -> ResponseEntity.ok(cards) }
}
