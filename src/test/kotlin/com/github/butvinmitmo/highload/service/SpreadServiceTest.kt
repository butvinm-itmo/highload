package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.dto.CreateSpreadRequest
import com.github.butvinmitmo.highload.entity.ArcanaType
import com.github.butvinmitmo.highload.entity.Card
import com.github.butvinmitmo.highload.entity.LayoutType
import com.github.butvinmitmo.highload.entity.Spread
import com.github.butvinmitmo.highload.entity.SpreadCard
import com.github.butvinmitmo.highload.entity.User
import com.github.butvinmitmo.highload.exception.ForbiddenException
import com.github.butvinmitmo.highload.exception.NotFoundException
import com.github.butvinmitmo.highload.mapper.LayoutTypeMapper
import com.github.butvinmitmo.highload.mapper.SpreadMapper
import com.github.butvinmitmo.highload.repository.LayoutTypeRepository
import com.github.butvinmitmo.highload.repository.SpreadCardRepository
import com.github.butvinmitmo.highload.repository.SpreadRepository
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
    private lateinit var userService: UserService

    @Mock
    private lateinit var cardService: CardService

    private lateinit var spreadService: SpreadService
    private val spreadMapper = SpreadMapper()
    private val layoutTypeMapper = LayoutTypeMapper()

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
                userService,
                cardService,
                spreadMapper,
                layoutTypeMapper,
            )
    }

    private fun createUser(
        id: UUID,
        username: String,
    ): User {
        val user = User(username = username)

        val idField = User::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)

        val createdAtField = User::class.java.getDeclaredField("createdAt")
        createdAtField.isAccessible = true
        createdAtField.set(user, Instant.now())

        return user
    }

    private fun createLayoutType(
        id: UUID,
        name: String,
        cardsCount: Int,
    ): LayoutType = LayoutType(id = id, name = name, cardsCount = cardsCount)

    private fun createArcanaType(
        id: UUID,
        name: String,
    ): ArcanaType = ArcanaType(id = id, name = name)

    private fun createCard(
        id: UUID,
        name: String = "Test Card",
    ): Card {
        val arcanaType = createArcanaType(UUID.randomUUID(), "MAJOR")
        return Card(id = id, name = name, arcanaType = arcanaType)
    }

    private fun createSpread(
        id: UUID,
        question: String,
        author: User,
        layoutType: LayoutType,
        spreadCards: List<SpreadCard> = emptyList(),
    ): Spread {
        val spread =
            Spread(
                question = question,
                author = author,
                layoutType = layoutType,
                spreadCards = spreadCards,
            )

        val idField = Spread::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(spread, id)

        val createdAtField = Spread::class.java.getDeclaredField("createdAt")
        createdAtField.isAccessible = true
        createdAtField.set(spread, createdAt)

        return spread
    }

    @Test
    fun `createSpread should create spread successfully`() {
        // Given
        val user = createUser(userId, "testuser")
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

        val savedSpread = createSpread(spreadId, "What will happen?", user, layoutType)

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
        val user = createUser(userId, "testuser")
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
        val user = createUser(userId, "testuser")
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)

        val spreads =
            listOf(
                createSpread(UUID.randomUUID(), "Question 1", user, layoutType),
                createSpread(UUID.randomUUID(), "Question 2", user, layoutType),
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
        val user = createUser(userId, "testuser")
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)

        val spreads =
            listOf(
                createSpread(UUID.randomUUID(), "Question 1", user, layoutType),
                createSpread(UUID.randomUUID(), "Question 2", user, layoutType),
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
        val user = createUser(userId, "testuser")
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val cursorId = UUID.randomUUID()

        val spreads =
            listOf(
                createSpread(UUID.randomUUID(), "Question 1", user, layoutType),
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
        val user = createUser(userId, "testuser")
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = createSpread(spreadId, "What will happen?", user, layoutType)

        whenever(spreadRepository.findByIdWithCardsAndInterpretations(spreadId)).thenReturn(spread)

        // When
        val result = spreadService.getSpread(spreadId)

        // Then
        assertNotNull(result)
        assertEquals("What will happen?", result.question)
    }

    @Test
    fun `getSpread should throw NotFoundException when spread not found`() {
        // Given
        whenever(spreadRepository.findByIdWithCardsAndInterpretations(spreadId)).thenReturn(null)

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
        val user = createUser(userId, "testuser")
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = createSpread(spreadId, "What will happen?", user, layoutType)

        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.of(spread))

        // When
        spreadService.deleteSpread(spreadId, userId)

        // Then
        verify(spreadRepository).deleteById(spreadId)
    }

    @Test
    fun `deleteSpread should throw ForbiddenException when user is not author`() {
        // Given
        val author = createUser(userId, "author")
        val otherUserId = UUID.randomUUID()
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = createSpread(spreadId, "What will happen?", author, layoutType)

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
    fun `getLayoutTypeByName should return layout type when found`() {
        // Given
        val layoutType = createLayoutType(layoutTypeId, "THREE_CARDS", 3)

        whenever(layoutTypeRepository.findByName("THREE_CARDS")).thenReturn(layoutType)

        // When
        val result = spreadService.getLayoutTypeByName("THREE_CARDS")

        // Then
        assertNotNull(result)
        assertEquals("THREE_CARDS", result.name)
        assertEquals(3, result.cardsCount)
    }

    @Test
    fun `getLayoutTypeByName should throw NotFoundException when not found`() {
        // Given
        whenever(layoutTypeRepository.findByName("INVALID")).thenReturn(null)

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                spreadService.getLayoutTypeByName("INVALID")
            }
        assertEquals("Layout type not found: INVALID", exception.message)
    }

    @Test
    fun `getAllLayoutTypes should return all layout types`() {
        // Given
        val layoutTypes =
            listOf(
                createLayoutType(UUID.randomUUID(), "ONE_CARD", 1),
                createLayoutType(UUID.randomUUID(), "THREE_CARDS", 3),
                createLayoutType(UUID.randomUUID(), "CROSS", 5),
            )

        whenever(layoutTypeRepository.findAll()).thenReturn(layoutTypes)

        // When
        val result = spreadService.getAllLayoutTypes()

        // Then
        assertNotNull(result)
        assertEquals(3, result.size)
    }

    @Test
    fun `getSpreadEntity should return spread when found`() {
        // Given
        val user = createUser(userId, "testuser")
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = createSpread(spreadId, "What will happen?", user, layoutType)

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
