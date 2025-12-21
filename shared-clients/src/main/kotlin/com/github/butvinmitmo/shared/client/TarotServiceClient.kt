package com.github.butvinmitmo.shared.client

import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.LayoutTypeDto
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@FeignClient(name = "tarot-service", url = "\${services.tarot-service.url:}")
interface TarotServiceClient {
    @GetMapping("/api/v0.0.1/cards")
    fun getCards(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestHeader("X-User-Role") role: String,
        @RequestParam(defaultValue = "0") page: Int = 0,
        @RequestParam(defaultValue = "50") size: Int = 50,
    ): ResponseEntity<List<CardDto>>

    @GetMapping("/api/v0.0.1/layout-types")
    fun getLayoutTypes(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestHeader("X-User-Role") role: String,
        @RequestParam(defaultValue = "0") page: Int = 0,
        @RequestParam(defaultValue = "50") size: Int = 50,
    ): ResponseEntity<List<LayoutTypeDto>>

    @GetMapping("/api/v0.0.1/cards/random")
    fun getRandomCards(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestHeader("X-User-Role") role: String,
        @RequestParam count: Int,
    ): ResponseEntity<List<CardDto>>

    @GetMapping("/api/v0.0.1/layout-types/{id}")
    fun getLayoutTypeById(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestHeader("X-User-Role") role: String,
        @PathVariable id: UUID,
    ): ResponseEntity<LayoutTypeDto>
}
