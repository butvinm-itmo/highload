package com.github.butvinmitmo.tarotservice.controller

import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.LayoutTypeDto
import com.github.butvinmitmo.tarotservice.service.TarotService
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/internal")
@Hidden
class InternalTarotController(
    private val tarotService: TarotService,
) {
    @GetMapping("/layout-types/{id}")
    fun getLayoutTypeById(
        @PathVariable id: UUID,
    ): ResponseEntity<LayoutTypeDto> {
        val layoutType = tarotService.getLayoutTypeDtoById(id)
        return ResponseEntity.ok(layoutType)
    }

    @GetMapping("/cards/random")
    fun getRandomCards(
        @RequestParam count: Int,
    ): ResponseEntity<List<CardDto>> {
        val cards = tarotService.getRandomCardDtos(count)
        return ResponseEntity.ok(cards)
    }
}
