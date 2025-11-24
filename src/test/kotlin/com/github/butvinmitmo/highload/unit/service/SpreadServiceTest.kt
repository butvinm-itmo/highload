package com.github.butvinmitmo.highload.unit.service

import com.github.butvinmitmo.highload.TestEntityFactory
import com.github.butvinmitmo.highload.dto.CreateSpreadRequest
import com.github.butvinmitmo.highload.entity.ArcanaType
import com.github.butvinmitmo.highload.entity.Card
import com.github.butvinmitmo.highload.entity.LayoutType
import com.github.butvinmitmo.highload.entity.Spread
import com.github.butvinmitmo.highload.entity.SpreadCard
import com.github.butvinmitmo.highload.exception.ForbiddenException
import com.github.butvinmitmo.highload.exception.NotFoundException
import com.github.butvinmitmo.highload.mapper.SpreadMapper
import com.github.butvinmitmo.highload.repository.InterpretationRepository
import com.github.butvinmitmo.highload.repository.LayoutTypeRepository
import com.github.butvinmitmo.highload.repository.SpreadCardRepository
import com.github.butvinmitmo.highload.repository.SpreadRepository
import com.github.butvinmitmo.highload.service.CardService
import com.github.butvinmitmo.highload.service.SpreadService
import com.github.butvinmitmo.highload.service.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SpreadServiceTest {
    @Mock
    private lateinit var spreadRepository: SpreadRepository

    @Mock
    private lateinit var spreadCardRepository: SpreadCardRepository

    @Mock
    private lateinit var layoutTypeRepository: LayoutTypeRepository

    @Mock
    private lateinit var interpretationRepository: InterpretationRepository

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var cardService: CardService

    private lateinit var spreadService: SpreadService
    private val spreadMapper = SpreadMapper()

    private val userId = UUID.randomUUID()
    private val spreadId = UUID.randomUUID()
    private val layoutTypeId = UUID.randomUUID()
    private val cardId1 = UUID.randomUUID()
    private val cardId2 = UUID.randomUUID()
    private val cardId3 = UUID.randomUUID()
    private val createdAt = Instant.now()

    @BeforeEach
    fun setup() {
        spreadService =
            SpreadService(
                spreadRepository,
                spreadCardRepository,
                layoutTypeRepository,
                interpretationRepository,
                userService,
                cardService,
                spreadMapper,
            )
    }

    private fun createLayoutType(
        id: UUID,
        name: String,
        cardsCount: Int,
    ): LayoutType {
        val layoutType = LayoutType(name = name, cardsCount = cardsCount)
        layoutType.id = id
        return layoutType
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
        name: String = "Test Card",
    ): Card {
        val arcanaType = createArcanaType(UUID.randomUUID(), "MAJOR")
        val card = Card(name = name, arcanaType = arcanaType)
        card.id = id
        return card
    }

    @Test
    fun `createSpread should create spread successfully`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "THREE_CARDS", 3)
        val cards =
            listOf(
                createCard(cardId1),
                createCard(cardId2),
                createCard(cardId3),
            )

        val request =
            CreateSpreadRequest(
                question = "What will happen?",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val savedSpread = TestEntityFactory.createSpread(spreadId, "What will happen?", user, layoutType, createdAt)

        whenever(userService.getUserEntity(userId)).thenReturn(user)
        whenever(layoutTypeRepository.findById(layoutTypeId)).thenReturn(Optional.of(layoutType))
        whenever(spreadRepository.save(any())).thenReturn(savedSpread)
        whenever(cardService.findRandomCards(3)).thenReturn(cards)
        whenever(spreadCardRepository.save(any())).thenAnswer { it.arguments[0] as SpreadCard }

        // When
        val result = spreadService.createSpread(request)

        // Then
        assertNotNull(result)
        assertEquals(spreadId, result.id)

        // Verify spread was saved
        val spreadCaptor = argumentCaptor<Spread>()
        verify(spreadRepository).save(spreadCaptor.capture())
        assertEquals("What will happen?", spreadCaptor.firstValue.question)
        assertEquals(user, spreadCaptor.firstValue.author)
        assertEquals(layoutType, spreadCaptor.firstValue.layoutType)

        // Verify 3 spread cards were created
        verify(spreadCardRepository, times(3)).save(any())
    }

    @Test
    fun `createSpread should throw NotFoundException when user not found`() {
        // Given
        val request =
            CreateSpreadRequest(
                question = "What will happen?",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        whenever(userService.getUserEntity(userId)).thenThrow(NotFoundException("User not found"))

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                spreadService.createSpread(request)
            }
        assertEquals("User not found", exception.message)

        // Verify spread was never saved
        verify(spreadRepository, never()).save(any())
    }

    @Test
    fun `createSpread should throw NotFoundException when layout type not found`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val request =
            CreateSpreadRequest(
                question = "What will happen?",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        whenever(userService.getUserEntity(userId)).thenReturn(user)
        whenever(layoutTypeRepository.findById(layoutTypeId)).thenReturn(Optional.empty())

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                spreadService.createSpread(request)
            }
        assertEquals("Layout type not found", exception.message)

        // Verify spread was never saved
        verify(spreadRepository, never()).save(any())
    }

    @Test
    fun `getSpreads should return paginated spreads`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)

        val spreads =
            listOf(
                TestEntityFactory.createSpread(UUID.randomUUID(), "Question 1", user, layoutType, createdAt),
                TestEntityFactory.createSpread(UUID.randomUUID(), "Question 2", user, layoutType, createdAt),
            )

        val pageable = PageRequest.of(0, 2)
        val page = PageImpl(spreads, pageable, 2)

        whenever(spreadRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(page)

        // When
        val result = spreadService.getSpreads(0, 2)

        // Then
        assertNotNull(result)
        assertEquals(2, result.content.size)
        assertEquals(2, result.totalElements)
        assertEquals(1, result.totalPages)
    }

    @Test
    fun `getSpreadsByScroll should return spreads when after is null`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)

        val spreads =
            listOf(
                TestEntityFactory.createSpread(UUID.randomUUID(), "Question 1", user, layoutType, createdAt),
                TestEntityFactory.createSpread(UUID.randomUUID(), "Question 2", user, layoutType, createdAt),
            )

        whenever(spreadRepository.findLatestSpreads(2)).thenReturn(spreads)

        // When
        val result = spreadService.getSpreadsByScroll(null, 2)

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
    }

    @Test
    fun `getSpreadsByScroll should return spreads after cursor`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val cursorId = UUID.randomUUID()

        val spreads =
            listOf(
                TestEntityFactory.createSpread(UUID.randomUUID(), "Question 1", user, layoutType, createdAt),
            )

        whenever(spreadRepository.findSpreadsAfterCursor(cursorId, 2)).thenReturn(spreads)

        // When
        val result = spreadService.getSpreadsByScroll(cursorId, 2)

        // Then
        assertNotNull(result)
        assertEquals(1, result.size)
    }

    @Test
    fun `getSpread should return spread when found`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = TestEntityFactory.createSpread(spreadId, "What will happen?", user, layoutType, createdAt)

        whenever(spreadRepository.findByIdWithCards(spreadId)).thenReturn(spread)
        whenever(interpretationRepository.findBySpreadIdOrderByCreatedAtDesc(spreadId)).thenReturn(emptyList())

        // When
        val result = spreadService.getSpread(spreadId)

        // Then
        assertNotNull(result)
        assertEquals("What will happen?", result.question)
    }

    @Test
    fun `getSpread should throw NotFoundException when spread not found`() {
        // Given
        whenever(spreadRepository.findByIdWithCards(spreadId)).thenReturn(null)

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                spreadService.getSpread(spreadId)
            }
        assertEquals("Spread not found", exception.message)
    }

    @Test
    fun `deleteSpread should delete spread when user is author`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = TestEntityFactory.createSpread(spreadId, "What will happen?", user, layoutType, createdAt)

        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.of(spread))

        // When
        spreadService.deleteSpread(spreadId, userId)

        // Then
        verify(spreadRepository).deleteById(spreadId)
    }

    @Test
    fun `deleteSpread should throw ForbiddenException when user is not author`() {
        // Given
        val author = TestEntityFactory.createUser(userId, "author", createdAt)
        val otherUserId = UUID.randomUUID()
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = TestEntityFactory.createSpread(spreadId, "What will happen?", author, layoutType, createdAt)

        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.of(spread))

        // When/Then
        val exception =
            assertThrows<ForbiddenException> {
                spreadService.deleteSpread(spreadId, otherUserId)
            }
        assertEquals("You can only delete your own spreads", exception.message)

        // Verify delete was never called
        verify(spreadRepository, never()).deleteById(any())
    }

    @Test
    fun `deleteSpread should throw NotFoundException when spread not found`() {
        // Given
        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.empty())

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                spreadService.deleteSpread(spreadId, userId)
            }
        assertEquals("Spread not found", exception.message)

        // Verify delete was never called
        verify(spreadRepository, never()).deleteById(any())
    }

    @Test
    fun `getLayoutTypeById should return layout type when found`() {
        // Given
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)

        whenever(layoutTypeRepository.findById(layoutTypeId)).thenReturn(Optional.of(layoutType))

        // When
        val result = spreadService.getLayoutTypeById(layoutTypeId)

        // Then
        assertNotNull(result)
        assertEquals("ONE_CARD", result.name)
        assertEquals(1, result.cardsCount)
    }

    @Test
    fun `getLayoutTypeById should throw NotFoundException when not found`() {
        // Given
        whenever(layoutTypeRepository.findById(layoutTypeId)).thenReturn(Optional.empty())

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                spreadService.getLayoutTypeById(layoutTypeId)
            }
        assertEquals("Layout type not found", exception.message)
    }

    @Test
    fun `getSpreadEntity should return spread when found`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = TestEntityFactory.createSpread(spreadId, "What will happen?", user, layoutType, createdAt)

        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.of(spread))

        // When
        val result = spreadService.getSpreadEntity(spreadId)

        // Then
        assertNotNull(result)
        assertEquals("What will happen?", result.question)
    }

    @Test
    fun `getSpreadEntity should throw NotFoundException when spread not found`() {
        // Given
        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.empty())

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                spreadService.getSpreadEntity(spreadId)
            }
        assertEquals("Spread not found", exception.message)
    }
}
