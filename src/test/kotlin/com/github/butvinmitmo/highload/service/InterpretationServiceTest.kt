package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.dto.CreateInterpretationRequest
import com.github.butvinmitmo.highload.dto.UpdateInterpretationRequest
import com.github.butvinmitmo.highload.entity.Interpretation
import com.github.butvinmitmo.highload.entity.LayoutType
import com.github.butvinmitmo.highload.entity.Spread
import com.github.butvinmitmo.highload.entity.User
import com.github.butvinmitmo.highload.exception.ConflictException
import com.github.butvinmitmo.highload.exception.ForbiddenException
import com.github.butvinmitmo.highload.exception.NotFoundException
import com.github.butvinmitmo.highload.mapper.InterpretationMapper
import com.github.butvinmitmo.highload.repository.InterpretationRepository
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InterpretationServiceTest {
    @Mock
    private lateinit var interpretationRepository: InterpretationRepository

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var spreadService: SpreadService

    private lateinit var interpretationService: InterpretationService
    private val interpretationMapper = InterpretationMapper()

    private val userId = UUID.randomUUID()
    private val spreadId = UUID.randomUUID()
    private val interpretationId = UUID.randomUUID()
    private val layoutTypeId = UUID.randomUUID()
    private val createdAt = Instant.now()

    @BeforeEach
    fun setup() {
        interpretationService =
            InterpretationService(
                interpretationRepository,
                userService,
                spreadService,
                interpretationMapper,
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

    private fun createSpread(
        id: UUID,
        question: String,
        author: User,
        layoutType: LayoutType,
    ): Spread {
        val spread =
            Spread(
                question = question,
                author = author,
                layoutType = layoutType,
            )

        val idField = Spread::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(spread, id)

        val createdAtField = Spread::class.java.getDeclaredField("createdAt")
        createdAtField.isAccessible = true
        createdAtField.set(spread, createdAt)

        return spread
    }

    private fun createInterpretation(
        id: UUID,
        text: String,
        author: User,
        spread: Spread,
    ): Interpretation {
        val interpretation =
            Interpretation(
                text = text,
                author = author,
                spread = spread,
            )

        val idField = Interpretation::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(interpretation, id)

        val createdAtField = Interpretation::class.java.getDeclaredField("createdAt")
        createdAtField.isAccessible = true
        createdAtField.set(interpretation, createdAt)

        return interpretation
    }

    @Test
    fun `addInterpretation should create interpretation successfully`() {
        // Given
        val user = createUser(userId, "testuser")
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = createSpread(spreadId, "What will happen?", user, layoutType)

        val request =
            CreateInterpretationRequest(
                text = "This card means...",
                authorId = userId,
            )

        val savedInterpretation = createInterpretation(interpretationId, "This card means...", user, spread)

        whenever(spreadService.getSpreadEntity(spreadId)).thenReturn(spread)
        whenever(userService.getUserEntity(userId)).thenReturn(user)
        whenever(interpretationRepository.existsByAuthorAndSpread(userId, spreadId)).thenReturn(false)
        whenever(interpretationRepository.save(any())).thenReturn(savedInterpretation)

        // When
        val result = interpretationService.addInterpretation(spreadId, request)

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

        whenever(spreadService.getSpreadEntity(spreadId)).thenThrow(NotFoundException("Spread not found"))

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                interpretationService.addInterpretation(spreadId, request)
            }
        assertEquals("Spread not found", exception.message)

        // Verify save was never called
        verify(interpretationRepository, never()).save(any())
    }

    @Test
    fun `addInterpretation should throw NotFoundException when user not found`() {
        // Given
        val user = createUser(userId, "testuser")
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = createSpread(spreadId, "What will happen?", user, layoutType)

        val request =
            CreateInterpretationRequest(
                text = "This card means...",
                authorId = userId,
            )

        whenever(spreadService.getSpreadEntity(spreadId)).thenReturn(spread)
        whenever(userService.getUserEntity(userId)).thenThrow(NotFoundException("User not found"))

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                interpretationService.addInterpretation(spreadId, request)
            }
        assertEquals("User not found", exception.message)

        // Verify save was never called
        verify(interpretationRepository, never()).save(any())
    }

    @Test
    fun `addInterpretation should throw ConflictException when user already has interpretation`() {
        // Given
        val user = createUser(userId, "testuser")
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = createSpread(spreadId, "What will happen?", user, layoutType)

        val request =
            CreateInterpretationRequest(
                text = "This card means...",
                authorId = userId,
            )

        whenever(spreadService.getSpreadEntity(spreadId)).thenReturn(spread)
        whenever(userService.getUserEntity(userId)).thenReturn(user)
        whenever(interpretationRepository.existsByAuthorAndSpread(userId, spreadId)).thenReturn(true)

        // When/Then
        val exception =
            assertThrows<ConflictException> {
                interpretationService.addInterpretation(spreadId, request)
            }
        assertEquals("You already have an interpretation for this spread", exception.message)

        // Verify save was never called
        verify(interpretationRepository, never()).save(any())
    }

    @Test
    fun `updateInterpretation should update interpretation successfully`() {
        // Given
        val user = createUser(userId, "testuser")
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = createSpread(spreadId, "What will happen?", user, layoutType)
        val interpretation = createInterpretation(interpretationId, "Old text", user, spread)

        val request = UpdateInterpretationRequest(text = "New text")

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.of(interpretation))
        whenever(interpretationRepository.save(any())).thenAnswer { invocation ->
            val saved = invocation.arguments[0] as Interpretation
            saved
        }

        // When
        val result = interpretationService.updateInterpretation(spreadId, interpretationId, userId, request)

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
        val request = UpdateInterpretationRequest(text = "New text")

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.empty())

        // When/Then
        val exception =
            assertThrows<NotFoundException> {
                interpretationService.updateInterpretation(spreadId, interpretationId, userId, request)
            }
        assertEquals("Interpretation not found", exception.message)

        // Verify save was never called
        verify(interpretationRepository, never()).save(any())
    }

    @Test
    fun `updateInterpretation should throw ForbiddenException when user is not author`() {
        // Given
        val author = createUser(userId, "author")
        val otherUserId = UUID.randomUUID()
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = createSpread(spreadId, "What will happen?", author, layoutType)
        val interpretation = createInterpretation(interpretationId, "Old text", author, spread)

        val request = UpdateInterpretationRequest(text = "New text")

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.of(interpretation))

        // When/Then
        val exception =
            assertThrows<ForbiddenException> {
                interpretationService.updateInterpretation(spreadId, interpretationId, otherUserId, request)
            }
        assertEquals("You can only edit your own interpretations", exception.message)

        // Verify save was never called
        verify(interpretationRepository, never()).save(any())
    }

    @Test
    fun `deleteInterpretation should delete interpretation successfully`() {
        // Given
        val user = createUser(userId, "testuser")
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = createSpread(spreadId, "What will happen?", user, layoutType)
        val interpretation = createInterpretation(interpretationId, "Text", user, spread)

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.of(interpretation))

        // When
        interpretationService.deleteInterpretation(spreadId, interpretationId, userId)

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
                interpretationService.deleteInterpretation(spreadId, interpretationId, userId)
            }
        assertEquals("Interpretation not found", exception.message)

        // Verify delete was never called
        verify(interpretationRepository, never()).deleteById(any())
    }

    @Test
    fun `deleteInterpretation should throw ForbiddenException when user is not author`() {
        // Given
        val author = createUser(userId, "author")
        val otherUserId = UUID.randomUUID()
        val layoutType = createLayoutType(layoutTypeId, "ONE_CARD", 1)
        val spread = createSpread(spreadId, "What will happen?", author, layoutType)
        val interpretation = createInterpretation(interpretationId, "Text", author, spread)

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.of(interpretation))

        // When/Then
        val exception =
            assertThrows<ForbiddenException> {
                interpretationService.deleteInterpretation(spreadId, interpretationId, otherUserId)
            }
        assertEquals("You can only delete your own interpretations", exception.message)

        // Verify delete was never called
        verify(interpretationRepository, never()).deleteById(any())
    }
}
