package com.github.butvinmitmo.divinationservice.client

import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.LayoutTypeDto
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@FeignClient(name = "tarot-service", url = "\${services.tarot-service.url:}")
interface TarotClient {
    @GetMapping("/api/internal/layout-types/{id}")
    fun getLayoutTypeById(
        @PathVariable id: UUID,
    ): ResponseEntity<LayoutTypeDto>

    @GetMapping("/api/internal/cards/random")
    fun getRandomCards(
        @RequestParam count: Int,
    ): ResponseEntity<List<CardDto>>
}
