package com.github.butvinmitmo.highload.controller

import com.github.butvinmitmo.highload.dto.CreateSpreadRequest
import com.github.butvinmitmo.highload.dto.DeleteRequest
import com.github.butvinmitmo.highload.dto.SpreadDto
import com.github.butvinmitmo.highload.dto.SpreadSummaryDto
import com.github.butvinmitmo.highload.service.SpreadService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v0.0.1/spreads")
class SpreadController(
    private val spreadService: SpreadService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createSpread(
        @RequestBody request: CreateSpreadRequest,
    ): SpreadDto = spreadService.createSpread(request)

    @GetMapping
    fun getSpreads(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<List<SpreadSummaryDto>> {
        val response = spreadService.getSpreads(page, size)
        return ResponseEntity
            .ok()
            .header("X-Total-Count", response.totalElements.toString())
            .body(response.content)
    }

    @GetMapping("/scroll")
    fun getSpreadsByScroll(
        @RequestParam(required = false) after: UUID?,
        @RequestParam(defaultValue = "20") size: Int,
    ): List<SpreadSummaryDto> = spreadService.getSpreadsByScroll(after, size)

    @GetMapping("/{id}")
    fun getSpread(
        @PathVariable id: UUID,
    ): SpreadDto = spreadService.getSpread(id)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteSpread(
        @PathVariable id: UUID,
        @RequestBody request: DeleteRequest,
    ) {
        spreadService.deleteSpread(id, request.userId)
    }
}
