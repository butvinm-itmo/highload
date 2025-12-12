package com.github.butvinmitmo.tarotservice.integration.service

import com.github.butvinmitmo.tarotservice.exception.NotFoundException
import com.github.butvinmitmo.tarotservice.integration.BaseIntegrationTest
import com.github.butvinmitmo.tarotservice.service.TarotService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class TarotServiceIntegrationTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var tarotService: TarotService

    @Test
    fun `getCards should return paginated cards`() {
        val result = tarotService.getCards(0, 10).block()

        assertNotNull(result)
        assertTrue(result!!.content.isNotEmpty())
        assertTrue(result.totalElements > 0)
    }

    @Test
    fun `getLayoutTypes should return paginated layout types`() {
        val result = tarotService.getLayoutTypes(0, 10).block()

        assertNotNull(result)
        assertEquals(3, result!!.content.size)
        assertTrue(result.content.any { it.name == "ONE_CARD" })
        assertTrue(result.content.any { it.name == "THREE_CARDS" })
        assertTrue(result.content.any { it.name == "CROSS" })
    }

    @Test
    fun `getLayoutTypeById should return layout type`() {
        val oneCardLayoutId = UUID.fromString("30000000-0000-0000-0000-000000000001")

        val layoutType = tarotService.getLayoutTypeById(oneCardLayoutId).block()

        assertNotNull(layoutType)
        assertEquals("ONE_CARD", layoutType!!.name)
        assertEquals(1, layoutType.cardsCount)
    }

    @Test
    fun `getLayoutTypeById should throw NotFoundException for non-existent id`() {
        val nonExistentId = UUID.randomUUID()

        assertThrows<NotFoundException> {
            tarotService.getLayoutTypeById(nonExistentId).block()
        }
    }

    @Test
    fun `getRandomCards should return requested number of cards`() {
        val cards = tarotService.getRandomCards(3).block()

        assertEquals(3, cards!!.size)
    }

    @Test
    fun `getRandomCardDtos should return requested number of cards as DTOs`() {
        val cardDtos = tarotService.getRandomCardDtos(5).block()

        assertEquals(5, cardDtos!!.size)
        cardDtos.forEach { card ->
            assertNotNull(card.id)
            assertNotNull(card.name)
            assertNotNull(card.arcanaType)
        }
    }
}
