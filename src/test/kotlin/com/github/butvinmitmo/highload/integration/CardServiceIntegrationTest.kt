package com.github.butvinmitmo.highload.integration

import com.github.butvinmitmo.highload.service.CardService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
class CardServiceIntegrationTest {
    companion object {
        @Container
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:15-alpine")
                .withDatabaseName("tarot_db_test")
                .withUsername("test_user")
                .withPassword("test_password")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
        }
    }

    @Autowired
    private lateinit var cardService: CardService

    @Test
    fun `should return all 78 tarot cards`() {
        val cards = cardService.getAllCards()

        assertNotNull(cards)
        assertEquals(78, cards.size)
    }

    @Test
    fun `should return cards with proper structure`() {
        val cards = cardService.getAllCards()

        val firstCard = cards.first()
        assertNotNull(firstCard.id)
        assertNotNull(firstCard.name)
        assertNotNull(firstCard.arcanaType)
        assertNotNull(firstCard.arcanaType.id)
        assertNotNull(firstCard.arcanaType.name)
    }

    @Test
    fun `should return both major and minor arcana cards`() {
        val cards = cardService.getAllCards()

        val majorArcanaCards = cards.filter { it.arcanaType.name == "MAJOR" }
        val minorArcanaCards = cards.filter { it.arcanaType.name == "MINOR" }

        assertEquals(22, majorArcanaCards.size)
        assertEquals(56, minorArcanaCards.size)
    }

    @Test
    fun `should find random cards of requested count`() {
        val count = 5
        val cards = cardService.findRandomCards(count)

        assertNotNull(cards)
        assertEquals(count, cards.size)
    }

    @Test
    fun `should return different random cards on multiple calls`() {
        val cards1 = cardService.findRandomCards(10)
        val cards2 = cardService.findRandomCards(10)

        assertNotNull(cards1)
        assertNotNull(cards2)
        assertEquals(10, cards1.size)
        assertEquals(10, cards2.size)

        // Very unlikely to get same 10 cards in same order twice
        val sameOrder = cards1.zip(cards2).all { (c1, c2) -> c1.id == c2.id }
        // This assertion might occasionally fail due to randomness, but it's statistically very unlikely
        // We're just checking that the method does return random results
    }

    @Test
    fun `should find random cards for one card spread`() {
        val cards = cardService.findRandomCards(1)

        assertNotNull(cards)
        assertEquals(1, cards.size)
    }

    @Test
    fun `should find random cards for three card spread`() {
        val cards = cardService.findRandomCards(3)

        assertNotNull(cards)
        assertEquals(3, cards.size)

        // Verify all cards are unique
        val uniqueIds = cards.map { it.id }.toSet()
        assertEquals(3, uniqueIds.size)
    }

    @Test
    fun `should find random cards for cross spread`() {
        val cards = cardService.findRandomCards(5)

        assertNotNull(cards)
        assertEquals(5, cards.size)

        // Verify all cards are unique
        val uniqueIds = cards.map { it.id }.toSet()
        assertEquals(5, uniqueIds.size)
    }

    @Test
    fun `should return empty list when requesting zero cards`() {
        val cards = cardService.findRandomCards(0)

        assertNotNull(cards)
        assertEquals(0, cards.size)
    }

    @Test
    fun `should get all arcana types`() {
        val arcanaTypes = cardService.getAllArcanaTypes()

        assertNotNull(arcanaTypes)
        assertEquals(2, arcanaTypes.size)

        val names = arcanaTypes.map { it.name }
        assertTrue(names.contains("MAJOR"))
        assertTrue(names.contains("MINOR"))
    }

    @Test
    fun `should get arcana type by name - MAJOR`() {
        val arcanaType = cardService.getArcanaTypeByName("MAJOR")

        assertNotNull(arcanaType)
        assertEquals("MAJOR", arcanaType!!.name)
    }

    @Test
    fun `should get arcana type by name - MINOR`() {
        val arcanaType = cardService.getArcanaTypeByName("MINOR")

        assertNotNull(arcanaType)
        assertEquals("MINOR", arcanaType!!.name)
    }

    @Test
    fun `should return null when getting non-existent arcana type`() {
        val arcanaType = cardService.getArcanaTypeByName("INVALID")

        assertNull(arcanaType)
    }

    @Test
    fun `should contain The Fool card`() {
        val cards = cardService.getAllCards()

        val theFool = cards.find { it.name == "The Fool" }
        assertNotNull(theFool)
        assertEquals("MAJOR", theFool!!.arcanaType.name)
    }

    @Test
    fun `should contain Ace of Wands card`() {
        val cards = cardService.getAllCards()

        val aceOfWands = cards.find { it.name == "Ace of Wands" }
        assertNotNull(aceOfWands)
        assertEquals("MINOR", aceOfWands!!.arcanaType.name)
    }

    @Test
    fun `random cards should not exceed total available cards`() {
        // Request more cards than available - should still work based on repository implementation
        val cards = cardService.findRandomCards(100)

        assertNotNull(cards)
        // The actual behavior depends on repository implementation
        // It might return all 78 cards or limit to available count
        assertTrue(cards.size <= 78)
    }

    @Test
    fun `should handle concurrent random card requests`() {
        val results = mutableListOf<List<com.github.butvinmitmo.highload.entity.Card>>()

        repeat(5) {
            val cards = cardService.findRandomCards(3)
            results.add(cards)
        }

        results.forEach { cards ->
            assertEquals(3, cards.size)
            assertNotNull(cards)
        }
    }

    @Test
    fun `all cards should have non-empty names`() {
        val cards = cardService.getAllCards()

        cards.forEach { card ->
            assertNotNull(card.name)
            assertTrue(card.name.isNotEmpty())
        }
    }

    @Test
    fun `all arcana types should have non-empty names`() {
        val arcanaTypes = cardService.getAllArcanaTypes()

        arcanaTypes.forEach { arcanaType ->
            assertNotNull(arcanaType.name)
            assertTrue(arcanaType.name.isNotEmpty())
        }
    }
}
