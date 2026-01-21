package com.github.butvinmitmo.shared.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

/**
 * Internal Feign client for calling divination-service internal endpoints.
 * Used by notification-service to get spread owner for notification targeting.
 * This client bypasses the gateway and calls divination-service directly via Eureka.
 */
@FeignClient(
    name = "divination-service",
    contextId = "divinationServiceInternalClient",
    url = "\${services.divination-service.url:}",
    fallbackFactory = DivinationServiceInternalFallbackFactory::class,
)
interface DivinationServiceInternalClient {
    /**
     * Gets the author (owner) ID of a spread.
     * Used by notification-service to determine who should receive notifications.
     *
     * @param spreadId The ID of the spread
     * @return 200 OK with author UUID, or 404 if spread not found
     */
    @GetMapping("/internal/spreads/{spreadId}/owner")
    fun getSpreadOwner(
        @PathVariable spreadId: UUID,
    ): ResponseEntity<UUID>
}
