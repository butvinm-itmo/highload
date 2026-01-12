package com.github.butvinmitmo.shared.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

/**
 * Internal Feign client for calling divination-service cleanup endpoints.
 * Used by user-service to delete user data before deleting the user.
 * This client bypasses the gateway and calls divination-service directly via Eureka.
 */
@FeignClient(name = "divination-service-internal", url = "\${services.divination-service.url:}")
interface DivinationServiceInternalClient {
    /**
     * Deletes all data associated with a user (spreads and interpretations).
     * Should be called before deleting a user to ensure cascade cleanup.
     *
     * @param userId The ID of the user whose data should be deleted
     * @return 204 No Content on success
     */
    @DeleteMapping("/internal/users/{userId}/data")
    fun deleteUserData(
        @PathVariable userId: UUID,
    ): ResponseEntity<Void>
}
