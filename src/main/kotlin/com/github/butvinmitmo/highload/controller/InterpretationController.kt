package com.github.butvinmitmo.highload.controller

import com.github.butvinmitmo.highload.dto.CreateInterpretationRequest
import com.github.butvinmitmo.highload.dto.DeleteRequest
import com.github.butvinmitmo.highload.dto.InterpretationDto
import com.github.butvinmitmo.highload.dto.UpdateInterpretationRequest
import com.github.butvinmitmo.highload.service.InterpretationService
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
class InterpretationController(
    private val interpretationService: InterpretationService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addInterpretation(
        @PathVariable spreadId: UUID,
        @RequestBody request: CreateInterpretationRequest,
    ): InterpretationDto = interpretationService.addInterpretation(spreadId, request)

    @PutMapping("/{id}")
    fun updateInterpretation(
        @PathVariable spreadId: UUID,
        @PathVariable id: UUID,
        @RequestBody request: UpdateInterpretationRequest,
    ): InterpretationDto {
        // TODO: Get userId from authentication context
        val userId = UUID.randomUUID() // Placeholder for now
        return interpretationService.updateInterpretation(spreadId, id, userId, request)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteInterpretation(
        @PathVariable spreadId: UUID,
        @PathVariable id: UUID,
        @RequestBody request: DeleteRequest,
    ) {
        interpretationService.deleteInterpretation(spreadId, id, request.userId)
    }
}
