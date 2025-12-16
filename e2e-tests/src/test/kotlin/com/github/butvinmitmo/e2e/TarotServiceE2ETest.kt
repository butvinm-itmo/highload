package com.github.butvinmitmo.e2e

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.util.UUID

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TarotServiceE2ETest : BaseE2ETest() {
    companion object {
        private lateinit var oneCardLayoutId: UUID
    }

    @Test
    @Order(1)
    fun `GET cards should return paginated list with 50 cards per page`() {
        loginAsAdmin()
        val response = tarotClient.getCards(page = 0, size = 50)

        assertEquals(200, response.statusCode.value())
        val cards = response.body!!
        assertEquals(50, cards.size, "First page should return 50 cards")
    }

    @Test
    @Order(2)
    fun `Total cards across pages should be 78`() {
        loginAsAdmin()
        val page1 = tarotClient.getCards(page = 0, size = 50).body!!
        val page2 = tarotClient.getCards(page = 1, size = 50).body!!

        assertEquals(78, page1.size + page2.size, "Total tarot cards should be 78")
    }

    @Test
    @Order(3)
    fun `Cards should have proper structure with name and arcana type`() {
        loginAsAdmin()
        val cards = tarotClient.getCards(page = 0, size = 1).body!!
        val card = cards.first()

        assertNotNull(card.id, "Card should have an ID")
        assertNotNull(card.name, "Card should have a name")
        assertNotNull(card.arcanaType, "Card should have an arcana type")
        assertNotNull(card.arcanaType.name, "Arcana type should have a name")
    }

    @Test
    @Order(4)
    fun `GET layout types should return at least 3 types`() {
        loginAsAdmin()
        val response = tarotClient.getLayoutTypes()

        assertEquals(200, response.statusCode.value())
        val layoutTypes = response.body!!
        assertTrue(layoutTypes.size >= 3, "Should have at least 3 layout types")

        val oneCard = layoutTypes.find { it.name == "ONE_CARD" }
        assertNotNull(oneCard, "ONE_CARD layout should exist")
        oneCardLayoutId = oneCard!!.id
    }

    @Test
    @Order(5)
    fun `GET random cards should return requested count`() {
        loginAsAdmin()
        val response = tarotClient.getRandomCards(count = 3)

        assertEquals(200, response.statusCode.value())
        assertEquals(3, response.body?.size, "Should return exactly 3 random cards")
    }

    @Test
    @Order(6)
    fun `GET layout type by id should return correct type`() {
        loginAsAdmin()
        val response = tarotClient.getLayoutTypeById(oneCardLayoutId)

        assertEquals(200, response.statusCode.value())
        assertEquals("ONE_CARD", response.body?.name)
        assertEquals(1, response.body?.cardsCount)
    }
}
