package com.github.butvinmitmo.highload.unit.service

import com.github.butvinmitmo.highload.TestEntityFactory
import com.github.butvinmitmo.highload.dto.CreateInterpretationRequest
import com.github.butvinmitmo.highload.dto.CreateSpreadRequest
import com.github.butvinmitmo.highload.dto.UpdateInterpretationRequest
import com.github.butvinmitmo.highload.entity.ArcanaType
import com.github.butvinmitmo.highload.entity.Card
import com.github.butvinmitmo.highload.entity.Interpretation
import com.github.butvinmitmo.highload.entity.LayoutType
import com.github.butvinmitmo.highload.entity.Spread
import com.github.butvinmitmo.highload.entity.SpreadCard
import com.github.butvinmitmo.highload.exception.ConflictException
import com.github.butvinmitmo.highload.exception.ForbiddenException
import com.github.butvinmitmo.highload.exception.NotFoundException
import com.github.butvinmitmo.highload.mapper.InterpretationMapper
import com.github.butvinmitmo.highload.mapper.SpreadMapper
import com.github.butvinmitmo.highload.repository.InterpretationRepository
import com.github.butvinmitmo.highload.repository.SpreadCardRepository
import com.github.butvinmitmo.highload.repository.SpreadRepository
import com.github.butvinmitmo.highload.service.DivinationService
import com.github.butvinmitmo.highload.service.TarotService
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
class DivinationServiceTest {
    @Mock
    private lateinit var spreadRepository: SpreadRepository

    @Mock
    private lateinit var spreadCardRepository: SpreadCardRepository

    @Mock
    private lateinit var interpretationRepository: InterpretationRepository

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var tarotService: TarotService

    private lateinit var divinationService: DivinationService
    private val spreadMapper = SpreadMapper()
    private val interpretationMapper = InterpretationMapper()

    private val userId = UUID.randomUUID()
    private val spreadId = UUID.randomUUID()
    private val interpretationId = UUID.randomUUID()
    private val layoutTypeId = UUID.randomUUID()
    private val cardId1 = UUID.randomUUID()
    private val cardId2 = UUID.randomUUID()
    private val cardId3 = UUID.randomUUID()
    private val createdAt = Instant.now()

    @BeforeEach
    fun setup() {
        divinationService =
            DivinationService(
                spreadRepository,
                spreadCardRepository,
                interpretationRepository,
                userService,
                tarotService,
                spreadMapper,
                interpretationMapper,
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

    // ==================== Spread Tests ====================

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
        whenever(tarotService.getLayoutTypeById(layoutTypeId)).thenReturn(layoutType)
        whenever(spreadRepository.save(any())).thenReturn(savedSpread)
        whenever(tarotService.getRandomCards(3)).thenReturn(cards)
        whenever(spreadCardRepository.save(any())).thenAnswer { it.arguments[0] as SpreadCard }

        // When
        val result = divinationService.createSpread(request)

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
                divinationService.createSpread(request)
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
        whenever(tarotService.getLayoutTypeById(layoutTypeId)).thenThrow(NotFoundException("Layout type not found"))

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                divinationService.createSpread(request)
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
        val result = divinationService.getSpreads(0, 2)

        // Then
        assertNotNull(result)
        assertEquals(2, result.content.size)
        assertEquals(2, result.totalElements)
        assertEquals(1, result.totalPages)
    }

    @Test
    fun `getSpreadsByScroll should return spreads when after is null`() {
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)

        val spreads =
            listOf(
                TestEntityFactory.createSpread(UUID.randomUUID(), "Question 1", user, layoutType, createdAt),
                TestEntityFactory.createSpread(UUID.randomUUID(), "Question 2", user, layoutType, createdAt),
            )

        whenever(spreadRepository.findLatestSpreads(3)).thenReturn(spreads)

        val result = divinationService.getSpreadsByScroll(null, 2)

        assertNotNull(result)
        assertEquals(2, result.items.size)
        assertEquals(null, result.nextCursor)
    }

    @Test
    fun `getSpreadsByScroll should return spreads after cursor`() {
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val cursorId = UUID.randomUUID()

        val spreads =
            listOf(
                TestEntityFactory.createSpread(UUID.randomUUID(), "Question 1", user, layoutType, createdAt),
            )

        whenever(spreadRepository.findSpreadsAfterCursor(cursorId, 3)).thenReturn(spreads)

        val result = divinationService.getSpreadsByScroll(cursorId, 2)

        assertNotNull(result)
        assertEquals(1, result.items.size)
        assertEquals(null, result.nextCursor)
    }

    @Test
    fun `getSpreadsByScroll should return nextCursor when more items exist`() {
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val secondId = UUID.randomUUID()

        val spreads =
            listOf(
                TestEntityFactory.createSpread(UUID.randomUUID(), "Question 1", user, layoutType, createdAt),
                TestEntityFactory.createSpread(secondId, "Question 2", user, layoutType, createdAt),
                TestEntityFactory.createSpread(UUID.randomUUID(), "Question 3", user, layoutType, createdAt),
            )

        whenever(spreadRepository.findLatestSpreads(3)).thenReturn(spreads)

        val result = divinationService.getSpreadsByScroll(null, 2)

        assertNotNull(result)
        assertEquals(2, result.items.size)
        assertEquals(secondId, result.nextCursor)
    }

    @Test
    fun `getSpread should return spread when found`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = TestEntityFactory.createSpread(spreadId, "What will happen?", user, layoutType, createdAt)

        whenever(spreadRepository.findByIdWithCards(spreadId)).thenReturn(spread)
        whenever(interpretationRepository.findBySpreadIdOrderByCreatedAtDesc(any(), any()))
            .thenReturn(PageImpl(emptyList()))

        // When
        val result = divinationService.getSpread(spreadId)

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
                divinationService.getSpread(spreadId)
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
        divinationService.deleteSpread(spreadId, userId)

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
                divinationService.deleteSpread(spreadId, otherUserId)
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
                divinationService.deleteSpread(spreadId, userId)
            }
        assertEquals("Spread not found", exception.message)

        // Verify delete was never called
        verify(spreadRepository, never()).deleteById(any())
    }

    @Test
    fun `getSpreadEntity should return spread when found`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = TestEntityFactory.createSpread(spreadId, "What will happen?", user, layoutType, createdAt)

        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.of(spread))

        // When
        val result = divinationService.getSpreadEntity(spreadId)

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
                divinationService.getSpreadEntity(spreadId)
            }
        assertEquals("Spread not found", exception.message)
    }

    // ==================== Interpretation Tests ====================

    @Test
    fun `addInterpretation should create interpretation successfully`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = TestEntityFactory.createSpread(spreadId, "What will happen?", user, layoutType, createdAt)

        val request =
            CreateInterpretationRequest(
                text = "This card means...",
                authorId = userId,
            )

        val savedInterpretation =
            TestEntityFactory.createInterpretation(
                interpretationId,
                "This card means...",
                user,
                spread,
                createdAt,
            )

        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.of(spread))
        whenever(userService.getUserEntity(userId)).thenReturn(user)
        whenever(interpretationRepository.existsByAuthorAndSpread(userId, spreadId)).thenReturn(false)
        whenever(interpretationRepository.save(any())).thenReturn(savedInterpretation)

        // When
        val result = divinationService.addInterpretation(spreadId, request)

        // Then
        assertNotNull(result)
        assertEquals(interpretationId, result.id)

        // Verify interpretation was saved
        val interpretationCaptor = argumentCaptor<Interpretation>()
        verify(interpretationRepository).save(interpretationCaptor.capture())
        assertEquals("This card means...", interpretationCaptor.firstValue.text)
        assertEquals(user, interpretationCaptor.firstValue.author)
        assertEquals(spread, interpretationCaptor.firstValue.spread)
    }

    @Test
    fun `addInterpretation should throw NotFoundException when spread not found`() {
        // Given
        val request =
            CreateInterpretationRequest(
                text = "This card means...",
                authorId = userId,
            )

        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.empty())

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                divinationService.addInterpretation(spreadId, request)
            }
        assertEquals("Spread not found", exception.message)

        // Verify save was never called
        verify(interpretationRepository, never()).save(any())
    }

    @Test
    fun `addInterpretation should throw NotFoundException when user not found`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = TestEntityFactory.createSpread(spreadId, "What will happen?", user, layoutType, createdAt)

        val request =
            CreateInterpretationRequest(
                text = "This card means...",
                authorId = userId,
            )

        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.of(spread))
        whenever(userService.getUserEntity(userId)).thenThrow(NotFoundException("User not found"))

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                divinationService.addInterpretation(spreadId, request)
            }
        assertEquals("User not found", exception.message)

        // Verify save was never called
        verify(interpretationRepository, never()).save(any())
    }

    @Test
    fun `addInterpretation should throw ConflictException when user already has interpretation`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = TestEntityFactory.createSpread(spreadId, "What will happen?", user, layoutType, createdAt)

        val request =
            CreateInterpretationRequest(
                text = "This card means...",
                authorId = userId,
            )

        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.of(spread))
        whenever(userService.getUserEntity(userId)).thenReturn(user)
        whenever(interpretationRepository.existsByAuthorAndSpread(userId, spreadId)).thenReturn(true)

        // When/Then
        val exception =
            assertThrows<ConflictException> {
                divinationService.addInterpretation(spreadId, request)
            }
        assertEquals("You already have an interpretation for this spread", exception.message)

        // Verify save was never called
        verify(interpretationRepository, never()).save(any())
    }

    @Test
    fun `updateInterpretation should update interpretation successfully`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = TestEntityFactory.createSpread(spreadId, "What will happen?", user, layoutType, createdAt)
        val interpretation =
            TestEntityFactory.createInterpretation(
                interpretationId,
                "Old text",
                user,
                spread,
                createdAt,
            )

        val request = UpdateInterpretationRequest(text = "New text", authorId = userId)

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.of(interpretation))
        whenever(interpretationRepository.save(any())).thenAnswer { invocation ->
            val saved = invocation.arguments[0] as Interpretation
            saved
        }

        // When
        val result = divinationService.updateInterpretation(spreadId, interpretationId, userId, request)

        // Then
        assertNotNull(result)
        assertEquals("New text", result.text)

        // Verify interpretation was saved
        val interpretationCaptor = argumentCaptor<Interpretation>()
        verify(interpretationRepository).save(interpretationCaptor.capture())
        assertEquals("New text", interpretationCaptor.firstValue.text)
    }

    @Test
    fun `updateInterpretation should throw NotFoundException when interpretation not found`() {
        // Given
        val request = UpdateInterpretationRequest(text = "New text", authorId = userId)

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.empty())

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                divinationService.updateInterpretation(spreadId, interpretationId, userId, request)
            }
        assertEquals("Interpretation not found", exception.message)

        // Verify save was never called
        verify(interpretationRepository, never()).save(any())
    }

    @Test
    fun `updateInterpretation should throw ForbiddenException when user is not author`() {
        // Given
        val author = TestEntityFactory.createUser(userId, "author", createdAt)
        val otherUserId = UUID.randomUUID()
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = TestEntityFactory.createSpread(spreadId, "What will happen?", author, layoutType, createdAt)
        val interpretation =
            TestEntityFactory.createInterpretation(
                interpretationId,
                "Old text",
                author,
                spread,
                createdAt,
            )

        val request = UpdateInterpretationRequest(text = "New text", authorId = otherUserId)

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.of(interpretation))

        // When/Then
        val exception =
            assertThrows<ForbiddenException> {
                divinationService.updateInterpretation(spreadId, interpretationId, otherUserId, request)
            }
        assertEquals("You can only edit your own interpretations", exception.message)

        // Verify save was never called
        verify(interpretationRepository, never()).save(any())
    }

    @Test
    fun `deleteInterpretation should delete interpretation successfully`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = TestEntityFactory.createSpread(spreadId, "What will happen?", user, layoutType, createdAt)
        val interpretation = TestEntityFactory.createInterpretation(interpretationId, "Text", user, spread, createdAt)

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.of(interpretation))

        // When
        divinationService.deleteInterpretation(spreadId, interpretationId, userId)

        // Then
        verify(interpretationRepository).deleteById(interpretationId)
    }

    @Test
    fun `deleteInterpretation should throw NotFoundException when interpretation not found`() {
        // Given
        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.empty())

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                divinationService.deleteInterpretation(spreadId, interpretationId, userId)
            }
        assertEquals("Interpretation not found", exception.message)

        // Verify delete was never called
        verify(interpretationRepository, never()).deleteById(any())
    }

    @Test
    fun `deleteInterpretation should throw ForbiddenException when user is not author`() {
        // Given
        val author = TestEntityFactory.createUser(userId, "author", createdAt)
        val otherUserId = UUID.randomUUID()
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = TestEntityFactory.createSpread(spreadId, "What will happen?", author, layoutType, createdAt)
        val interpretation = TestEntityFactory.createInterpretation(interpretationId, "Text", author, spread, createdAt)

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.of(interpretation))

        // When/Then
        val exception =
            assertThrows<ForbiddenException> {
                divinationService.deleteInterpretation(spreadId, interpretationId, otherUserId)
            }
        assertEquals("You can only delete your own interpretations", exception.message)

        // Verify delete was never called
        verify(interpretationRepository, never()).deleteById(any())
    }

    @Test
    fun `getInterpretation should return interpretation successfully`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = TestEntityFactory.createSpread(spreadId, "What will happen?", user, layoutType, createdAt)
        val interpretation =
            TestEntityFactory.createInterpretation(
                interpretationId,
                "Test text",
                user,
                spread,
                createdAt,
            )

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.of(interpretation))

        // When
        val result = divinationService.getInterpretation(spreadId, interpretationId)

        // Then
        assertNotNull(result)
        assertEquals(interpretationId, result.id)
        assertEquals("Test text", result.text)
        assertEquals(userId, result.author.id)
    }

    @Test
    fun `getInterpretation should throw NotFoundException when interpretation not found`() {
        // Given
        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.empty())

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                divinationService.getInterpretation(spreadId, interpretationId)
            }
        assertEquals("Interpretation not found", exception.message)
    }

    @Test
    fun `getInterpretation should throw NotFoundException when interpretation belongs to different spread`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = TestEntityFactory.createSpread(spreadId, "What will happen?", user, layoutType, createdAt)
        val interpretation =
            TestEntityFactory.createInterpretation(
                interpretationId,
                "Test text",
                user,
                spread,
                createdAt,
            )

        val differentSpreadId = UUID.randomUUID()

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.of(interpretation))

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                divinationService.getInterpretation(differentSpreadId, interpretationId)
            }
        assertEquals("Interpretation not found in this spread", exception.message)
    }

    @Test
    fun `getInterpretations should return all interpretations for spread`() {
        // Given
        val user1 = TestEntityFactory.createUser(userId, "user1", createdAt)
        val user2 = TestEntityFactory.createUser(UUID.randomUUID(), "user2", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = TestEntityFactory.createSpread(spreadId, "What will happen?", user1, layoutType, createdAt)

        val interpretation1 =
            TestEntityFactory.createInterpretation(
                UUID.randomUUID(),
                "First interpretation",
                user1,
                spread,
                createdAt,
            )
        val interpretation2 =
            TestEntityFactory.createInterpretation(
                UUID.randomUUID(),
                "Second interpretation",
                user2,
                spread,
                createdAt,
            )

        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.of(spread))
        whenever(interpretationRepository.findBySpreadIdOrderByCreatedAtDesc(any(), any()))
            .thenReturn(PageImpl(listOf(interpretation2, interpretation1)))

        // When
        val result = divinationService.getInterpretations(spreadId, 0, 20)

        // Then
        assertNotNull(result)
        assertEquals(2, result.content.size)
        assertEquals("Second interpretation", result.content[0].text)
        assertEquals("First interpretation", result.content[1].text)
    }

    @Test
    fun `getInterpretations should return empty list when no interpretations exist`() {
        // Given
        val user = TestEntityFactory.createUser(userId, "testuser", createdAt)
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = TestEntityFactory.createSpread(spreadId, "What will happen?", user, layoutType, createdAt)

        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.of(spread))
        whenever(interpretationRepository.findBySpreadIdOrderByCreatedAtDesc(any(), any()))
            .thenReturn(PageImpl(emptyList()))

        // When
        val result = divinationService.getInterpretations(spreadId, 0, 20)

        // Then
        assertNotNull(result)
        assertEquals(0, result.content.size)
    }

    @Test
    fun `getInterpretations should throw NotFoundException when spread not found`() {
        // Given
        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.empty())

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                divinationService.getInterpretations(spreadId, 0, 20)
            }
        assertEquals("Spread not found", exception.message)
    }
}
