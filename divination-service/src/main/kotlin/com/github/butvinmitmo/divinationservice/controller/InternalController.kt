package com.github.butvinmitmo.divinationservice.controller

import com.github.butvinmitmo.divinationservice.service.DivinationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/v0.0.1/internal")
@Tag(name = "Internal", description = "Internal endpoints for inter-service communication")
class InternalController(
    private val divinationService: DivinationService,
) {
    @DeleteMapping("/users/{userId}/data")
    @Operation(
        summary = "Delete all user data",
        description =
            "Deletes all spreads and interpretations authored by the given user. " +
                "Called by user-service before deleting a user to maintain referential integrity.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "User data deleted successfully"),
        ],
    )
    fun deleteUserData(
        @PathVariable userId: UUID,
    ): Mono<ResponseEntity<Void>> =
        divinationService
            .deleteByAuthorId(userId)
            .then(Mono.just(ResponseEntity.noContent().build()))
}
