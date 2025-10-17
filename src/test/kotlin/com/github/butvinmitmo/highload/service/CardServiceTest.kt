package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.entity.ArcanaType
import com.github.butvinmitmo.highload.entity.Card
import com.github.butvinmitmo.highload.mapper.ArcanaTypeMapper
import com.github.butvinmitmo.highload.mapper.CardMapper
import com.github.butvinmitmo.highload.repository.ArcanaTypeRepository
import com.github.butvinmitmo.highload.repository.CardRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class CardServiceTest {
    @Mock
    private lateinit var cardRepository: CardRepository

    @Mock
    private lateinit var arcanaTypeRepository: ArcanaTypeRepository

    private lateinit var cardService: CardService
    private val arcanaTypeMapper = ArcanaTypeMapper()
    private val cardMapper = CardMapper(arcanaTypeMapper)

    private val cardId1 = UUID.randomUUID()
    private val cardId2 = UUID.randomUUID()
    private val cardId3 = UUID.randomUUID()
    private val arcanaTypeId1 = UUID.randomUUID()
    private val arcanaTypeId2 = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        cardService = CardService(cardRepository, arcanaTypeRepository, cardMapper, arcanaTypeMapper)
    }

    private fun createArcanaType(
        id: UUID,
        name: String,
    ): ArcanaType = ArcanaType(id = id, name = name)

    private fun createCard(
        id: UUID,
        name: String,
        arcanaType: ArcanaType,
    ): Card = Card(id = id, name = name, arcanaType = arcanaType)

    @Test
    fun `findRandomCards should return requested number of random cards`() {
        // Given
        val majorArcana = createArcanaType(arcanaTypeId1, "MAJOR")
        val minorArcana = createArcanaType(arcanaTypeId2, "MINOR")

        val randomCards =
            listOf(
                createCard(cardId1, "The Fool", majorArcana),
                createCard(cardId2, "The Magician", majorArcana),
                createCard(cardId3, "Ace of Wands", minorArcana),
            )

        whenever(cardRepository.findRandomCards(3)).thenReturn(randomCards)

        // When
        val result = cardService.findRandomCards(3)

        // Then
        assertNotNull(result)
        assertEquals(3, result.size)
        assertEquals("The Fool", result[0].name)
        assertEquals("The Magician", result[1].name)
        assertEquals("Ace of Wands", result[2].name)
    }

    @Test
    fun `findRandomCards should return empty list when count is zero`() {
        // Given
        whenever(cardRepository.findRandomCards(0)).thenReturn(emptyList())

        // When
        val result = cardService.findRandomCards(0)

        // Then
        assertNotNull(result)
        assertEquals(0, result.size)
    }

    @Test
    fun `findRandomCards should return single card when count is one`() {
        // Given
        val majorArcana = createArcanaType(arcanaTypeId1, "MAJOR")
        val card = createCard(cardId1, "The Fool", majorArcana)

        whenever(cardRepository.findRandomCards(1)).thenReturn(listOf(card))

        // When
        val result = cardService.findRandomCards(1)

        // Then
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("The Fool", result[0].name)
    }

    @Test
    fun `getAllCards should return all cards`() {
        // Given
        val majorArcana = createArcanaType(arcanaTypeId1, "MAJOR")
        val minorArcana = createArcanaType(arcanaTypeId2, "MINOR")

        val cards =
            listOf(
                createCard(cardId1, "The Fool", majorArcana),
                createCard(cardId2, "The Magician", majorArcana),
                createCard(cardId3, "Ace of Wands", minorArcana),
            )

        whenever(cardRepository.findAll()).thenReturn(cards)

        // When
        val result = cardService.getAllCards()

        // Then
        assertNotNull(result)
        assertEquals(3, result.size)
        assertEquals("The Fool", result[0].name)
        assertEquals("The Magician", result[1].name)
        assertEquals("Ace of Wands", result[2].name)
    }

    @Test
    fun `getAllCards should return empty list when no cards exist`() {
        // Given
        whenever(cardRepository.findAll()).thenReturn(emptyList())

        // When
        val result = cardService.getAllCards()

        // Then
        assertNotNull(result)
        assertEquals(0, result.size)
    }

    @Test
    fun `getAllArcanaTypes should return all arcana types`() {
        // Given
        val arcanaTypes =
            listOf(
                createArcanaType(arcanaTypeId1, "MAJOR"),
                createArcanaType(arcanaTypeId2, "MINOR"),
            )

        whenever(arcanaTypeRepository.findAll()).thenReturn(arcanaTypes)

        // When
        val result = cardService.getAllArcanaTypes()

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("MAJOR", result[0].name)
        assertEquals("MINOR", result[1].name)
    }

    @Test
    fun `getAllArcanaTypes should return empty list when no arcana types exist`() {
        // Given
        whenever(arcanaTypeRepository.findAll()).thenReturn(emptyList())

        // When
        val result = cardService.getAllArcanaTypes()

        // Then
        assertNotNull(result)
        assertEquals(0, result.size)
    }

    @Test
    fun `getArcanaTypeByName should return arcana type when found`() {
        // Given
        val arcanaType = createArcanaType(arcanaTypeId1, "MAJOR")

        whenever(arcanaTypeRepository.findByName("MAJOR")).thenReturn(arcanaType)

        // When
        val result = cardService.getArcanaTypeByName("MAJOR")

        // Then
        assertNotNull(result)
        assertEquals("MAJOR", result!!.name)
        assertEquals(arcanaTypeId1, result.id)
    }

    @Test
    fun `getArcanaTypeByName should return null when not found`() {
        // Given
        whenever(arcanaTypeRepository.findByName("INVALID")).thenReturn(null)

        // When
        val result = cardService.getArcanaTypeByName("INVALID")

        // Then
        assertNull(result)
    }

    @Test
    fun `findRandomCards should handle large count`() {
        // Given
        val majorArcana = createArcanaType(arcanaTypeId1, "MAJOR")
        val cards =
            (1..10).map { i ->
                createCard(UUID.randomUUID(), "Card $i", majorArcana)
            }

        whenever(cardRepository.findRandomCards(10)).thenReturn(cards)

        // When
        val result = cardService.findRandomCards(10)

        // Then
        assertNotNull(result)
        assertEquals(10, result.size)
    }

    @Test
    fun `getAllCards should preserve card arcana type information`() {
        // Given
        val majorArcana = createArcanaType(arcanaTypeId1, "MAJOR")
        val minorArcana = createArcanaType(arcanaTypeId2, "MINOR")

        val cards =
            listOf(
                createCard(cardId1, "The Fool", majorArcana),
                createCard(cardId2, "Ace of Wands", minorArcana),
            )

        whenever(cardRepository.findAll()).thenReturn(cards)

        // When
        val result = cardService.getAllCards()

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("MAJOR", result[0].arcanaType.name)
        assertEquals("MINOR", result[1].arcanaType.name)
    }
}
