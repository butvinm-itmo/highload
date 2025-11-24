package com.github.butvinmitmo.highload.unit.service

import com.github.butvinmitmo.highload.dto.ArcanaTypeDto
import com.github.butvinmitmo.highload.dto.CardDto
import com.github.butvinmitmo.highload.entity.ArcanaType
import com.github.butvinmitmo.highload.entity.Card
import com.github.butvinmitmo.highload.mapper.CardMapper
import com.github.butvinmitmo.highload.repository.CardRepository
import com.github.butvinmitmo.highload.service.CardService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class CardServiceTest {
    @Mock
    private lateinit var cardRepository: CardRepository

    @Mock
    private lateinit var cardMapper: CardMapper

    private lateinit var cardService: CardService

    private val cardId1 = UUID.randomUUID()
    private val cardId2 = UUID.randomUUID()
    private val cardId3 = UUID.randomUUID()
    private val arcanaTypeId1 = UUID.randomUUID()
    private val arcanaTypeId2 = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        cardService = CardService(cardRepository, cardMapper)
    }

    private fun createArcanaType(
        id: UUID,
        name: String,
    ): ArcanaType {
        val arcanaType = ArcanaType(name = name)
        arcanaType.id = id
        return arcanaType
    }

    private fun createCard(
        id: UUID,
        name: String,
        arcanaType: ArcanaType,
    ): Card {
        val card = Card(name = name, arcanaType = arcanaType)
        card.id = id
        return card
    }

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
    fun `getCards should return paginated list of card DTOs`() {
        // Given
        val majorArcana = createArcanaType(arcanaTypeId1, "MAJOR")
        val minorArcana = createArcanaType(arcanaTypeId2, "MINOR")

        val cards =
            listOf(
                createCard(cardId1, "The Fool", majorArcana),
                createCard(cardId2, "The Magician", majorArcana),
                createCard(cardId3, "Ace of Wands", minorArcana),
            )

        val page = PageImpl(cards, PageRequest.of(0, 3), 3)
        whenever(cardRepository.findAll(any<PageRequest>())).thenReturn(page)

        val arcanaTypeDto1 = ArcanaTypeDto(arcanaTypeId1, "MAJOR")
        val arcanaTypeDto2 = ArcanaTypeDto(arcanaTypeId2, "MINOR")

        whenever(cardMapper.toDto(cards[0])).thenReturn(CardDto(cardId1, "The Fool", arcanaTypeDto1))
        whenever(cardMapper.toDto(cards[1])).thenReturn(CardDto(cardId2, "The Magician", arcanaTypeDto1))
        whenever(cardMapper.toDto(cards[2])).thenReturn(CardDto(cardId3, "Ace of Wands", arcanaTypeDto2))

        // When
        val result = cardService.getCards(0, 3)

        // Then
        assertNotNull(result)
        assertEquals(3, result.size)
        assertEquals("The Fool", result[0].name)
        assertEquals("The Magician", result[1].name)
        assertEquals("Ace of Wands", result[2].name)
    }

    @Test
    fun `getCards should return empty list when no cards exist`() {
        // Given
        val page = PageImpl<Card>(emptyList(), PageRequest.of(0, 20), 0)
        whenever(cardRepository.findAll(any<PageRequest>())).thenReturn(page)

        // When
        val result = cardService.getCards(0, 20)

        // Then
        assertNotNull(result)
        assertEquals(0, result.size)
    }

    @Test
    fun `getCards should return correct page of cards`() {
        // Given
        val majorArcana = createArcanaType(arcanaTypeId1, "MAJOR")
        val card = createCard(cardId1, "The Fool", majorArcana)

        val page = PageImpl(listOf(card), PageRequest.of(1, 10), 15)
        whenever(cardRepository.findAll(any<PageRequest>())).thenReturn(page)

        val arcanaTypeDto = ArcanaTypeDto(arcanaTypeId1, "MAJOR")
        whenever(cardMapper.toDto(card)).thenReturn(CardDto(cardId1, "The Fool", arcanaTypeDto))

        // When
        val result = cardService.getCards(1, 10)

        // Then
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("The Fool", result[0].name)
    }
}
