package com.github.butvinmitmo.divinationservice.api.controller

import com.github.butvinmitmo.divinationservice.application.service.DivinationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * Internal API controller for service-to-service communication.
 * These endpoints are not exposed through the gateway and are only accessible
 * via Eureka service discovery for internal operations.
 */
@RestController
@RequestMapping("/internal")
class InternalController(
    private val divinationService: DivinationService,
) {
    /**
     * Deletes all data associated with a user (spreads and interpretations).
     * Called by user-service before deleting a user to ensure cascade cleanup.
     *
     * @param userId The ID of the user whose data should be deleted
     * @return 204 No Content on success
     */
    @DeleteMapping("/users/{userId}/data")
    fun deleteUserData(
        @PathVariable userId: UUID,
    ): Mono<ResponseEntity<Void>> =
        divinationService
            .deleteUserData(userId)
            .then(Mono.just(ResponseEntity.noContent().build()))

    /**
     * Gets the author (owner) ID of a spread.
     * Called by notification-service to determine who should receive notifications.
     *
     * @param spreadId The ID of the spread
     * @return 200 OK with author UUID, or 404 if spread not found
     */
    @GetMapping("/spreads/{spreadId}/owner")
    fun getSpreadOwner(
        @PathVariable spreadId: UUID,
    ): Mono<ResponseEntity<UUID>> =
        divinationService
            .getSpreadAuthorId(spreadId)
            .map { ResponseEntity.ok(it) }
            .defaultIfEmpty(ResponseEntity.notFound().build())
}
