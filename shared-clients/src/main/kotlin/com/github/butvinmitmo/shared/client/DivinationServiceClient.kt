package com.github.butvinmitmo.shared.client

import com.github.butvinmitmo.shared.dto.CreateInterpretationRequest
import com.github.butvinmitmo.shared.dto.CreateInterpretationResponse
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.CreateSpreadResponse
import com.github.butvinmitmo.shared.dto.DeleteRequest
import com.github.butvinmitmo.shared.dto.InterpretationDto
import com.github.butvinmitmo.shared.dto.SpreadDto
import com.github.butvinmitmo.shared.dto.SpreadSummaryDto
import com.github.butvinmitmo.shared.dto.UpdateInterpretationRequest
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@FeignClient(name = "divination-service", url = "\${services.divination-service.url:}")
interface DivinationServiceClient {
    @PostMapping("/api/v0.0.1/spreads")
    fun createSpread(
        @RequestBody request: CreateSpreadRequest,
    ): ResponseEntity<CreateSpreadResponse>

    @GetMapping("/api/v0.0.1/spreads")
    fun getSpreads(
        @RequestParam(defaultValue = "0") page: Int = 0,
        @RequestParam(defaultValue = "10") size: Int = 10,
    ): ResponseEntity<List<SpreadSummaryDto>>

    @GetMapping("/api/v0.0.1/spreads/scroll")
    fun getSpreadsScroll(
        @RequestParam(required = false) after: UUID? = null,
        @RequestParam(defaultValue = "10") size: Int = 10,
    ): ResponseEntity<List<SpreadSummaryDto>>

    @GetMapping("/api/v0.0.1/spreads/{id}")
    fun getSpreadById(
        @PathVariable id: UUID,
    ): ResponseEntity<SpreadDto>

    @DeleteMapping("/api/v0.0.1/spreads/{id}")
    fun deleteSpread(
        @PathVariable id: UUID,
        @RequestBody request: DeleteRequest,
    ): ResponseEntity<Void>

    @GetMapping("/api/v0.0.1/spreads/{spreadId}/interpretations")
    fun getInterpretations(
        @PathVariable spreadId: UUID,
    ): ResponseEntity<List<InterpretationDto>>

    @GetMapping("/api/v0.0.1/spreads/{spreadId}/interpretations/{id}")
    fun getInterpretation(
        @PathVariable spreadId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<InterpretationDto>

    @PostMapping("/api/v0.0.1/spreads/{spreadId}/interpretations")
    fun createInterpretation(
        @PathVariable spreadId: UUID,
        @RequestBody request: CreateInterpretationRequest,
    ): ResponseEntity<CreateInterpretationResponse>

    @PutMapping("/api/v0.0.1/spreads/{spreadId}/interpretations/{id}")
    fun updateInterpretation(
        @PathVariable spreadId: UUID,
        @PathVariable id: UUID,
        @RequestBody request: UpdateInterpretationRequest,
    ): ResponseEntity<InterpretationDto>

    @DeleteMapping("/api/v0.0.1/spreads/{spreadId}/interpretations/{id}")
    fun deleteInterpretation(
        @PathVariable spreadId: UUID,
        @PathVariable id: UUID,
        @RequestBody request: DeleteRequest,
    ): ResponseEntity<Void>

    /**
     * Internal endpoint: Deletes all spreads and interpretations authored by the given user.
     * Called by user-service before deleting a user to maintain referential integrity.
     */
    @DeleteMapping("/api/v0.0.1/internal/users/{userId}/data")
    fun deleteUserData(
        @PathVariable userId: UUID,
    ): ResponseEntity<Void>
}
