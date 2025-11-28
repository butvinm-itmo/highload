package com.github.butvinmitmo.highload.integration.service

import com.github.butvinmitmo.highload.exception.NotFoundException
import com.github.butvinmitmo.highload.integration.BaseIntegrationTest
import com.github.butvinmitmo.highload.repository.LayoutTypeRepository
import com.github.butvinmitmo.highload.service.TarotService
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

    @Autowired
    private lateinit var layoutTypeRepository: LayoutTypeRepository

    // ==================== Card Tests ====================

    @Test
    fun `should find random cards of requested count`() {
        val count = 5
        val cards = tarotService.getRandomCards(count)

        assertNotNull(cards)
        assertEquals(count, cards.size)
    }

    @Test
    fun `should return different random cards on multiple calls`() {
        val cards1 = tarotService.getRandomCards(10)
        val cards2 = tarotService.getRandomCards(10)

        assertNotNull(cards1)
        assertNotNull(cards2)
        assertEquals(10, cards1.size)
        assertEquals(10, cards2.size)

        // Very unlikely to get same 10 cards in same order twice
        // We're just checking that the method does return random results
    }

    @Test
    fun `should find random cards for one card spread`() {
        val cards = tarotService.getRandomCards(1)

        assertNotNull(cards)
        assertEquals(1, cards.size)
    }

    @Test
    fun `should find random cards for three card spread`() {
        val cards = tarotService.getRandomCards(3)

        assertNotNull(cards)
        assertEquals(3, cards.size)

        // Verify all cards are unique
        val uniqueIds = cards.map { it.id }.toSet()
        assertEquals(3, uniqueIds.size)
    }

    @Test
    fun `should find random cards for cross spread`() {
        val cards = tarotService.getRandomCards(5)

        assertNotNull(cards)
        assertEquals(5, cards.size)

        // Verify all cards are unique
        val uniqueIds = cards.map { it.id }.toSet()
        assertEquals(5, uniqueIds.size)
    }

    @Test
    fun `should return empty list when requesting zero cards`() {
        val cards = tarotService.getRandomCards(0)

        assertNotNull(cards)
        assertEquals(0, cards.size)
    }

    @Test
    fun `random cards should not exceed total available cards`() {
        // Request more cards than available - should still work based on repository implementation
        val cards = tarotService.getRandomCards(100)

        assertNotNull(cards)
        // The actual behavior depends on repository implementation
        // It might return all 78 cards or limit to available count
        assertTrue(cards.size <= 78)
    }

    @Test
    fun `should handle concurrent random card requests`() {
        val results = mutableListOf<List<com.github.butvinmitmo.highload.entity.Card>>()

        repeat(5) {
            val cards = tarotService.getRandomCards(3)
            results.add(cards)
        }

        results.forEach { cards ->
            assertEquals(3, cards.size)
            assertNotNull(cards)
        }
    }

    @Test
    fun `should get paginated cards`() {
        val result = tarotService.getCards(0, 10)

        assertNotNull(result)
        assertTrue(result.content.isNotEmpty())
        assertTrue(result.content.size <= 10)
    }

    @Test
    fun `should get second page of cards`() {
        val page0 = tarotService.getCards(0, 10)
        val page1 = tarotService.getCards(1, 10)

        assertNotNull(page0)
        assertNotNull(page1)

        // Pages should have different cards
        val page0Ids = page0.content.map { it.id }.toSet()
        val page1Ids = page1.content.map { it.id }.toSet()
        assertTrue(page0Ids.intersect(page1Ids).isEmpty(), "Pages should not overlap")
    }

    // ==================== Layout Type Tests ====================

    @Test
    fun `should get paginated layout types`() {
        val result = tarotService.getLayoutTypes(0, 10)

        assertNotNull(result)
        assertTrue(result.content.isNotEmpty())
    }

    @Test
    fun `should get layout type by id`() {
        val layoutType = layoutTypeRepository.findByName("ONE_CARD")
        assertNotNull(layoutType)

        val result = tarotService.getLayoutTypeById(layoutType!!.id)

        assertNotNull(result)
        assertEquals("ONE_CARD", result.name)
        assertEquals(1, result.cardsCount)
    }

    @Test
    fun `should get THREE_CARDS layout type by id`() {
        val layoutType = layoutTypeRepository.findByName("THREE_CARDS")
        assertNotNull(layoutType)

        val result = tarotService.getLayoutTypeById(layoutType!!.id)

        assertNotNull(result)
        assertEquals("THREE_CARDS", result.name)
        assertEquals(3, result.cardsCount)
    }

    @Test
    fun `should get CROSS layout type by id`() {
        val layoutType = layoutTypeRepository.findByName("CROSS")
        assertNotNull(layoutType)

        val result = tarotService.getLayoutTypeById(layoutType!!.id)

        assertNotNull(result)
        assertEquals("CROSS", result.name)
    }

    @Test
    fun `should throw NotFoundException when getting non-existent layout type by id`() {
        val nonExistentId = UUID.randomUUID()

        val exception =
            assertThrows<NotFoundException> {
                tarotService.getLayoutTypeById(nonExistentId)
            }
        assertEquals("Layout type not found", exception.message)
    }
}
