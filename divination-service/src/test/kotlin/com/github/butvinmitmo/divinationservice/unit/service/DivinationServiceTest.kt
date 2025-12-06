package com.github.butvinmitmo.divinationservice.unit.service

import com.github.butvinmitmo.divinationservice.TestEntityFactory
import com.github.butvinmitmo.divinationservice.client.TarotClient
import com.github.butvinmitmo.divinationservice.client.UserClient
import com.github.butvinmitmo.divinationservice.exception.ConflictException
import com.github.butvinmitmo.divinationservice.exception.ForbiddenException
import com.github.butvinmitmo.divinationservice.exception.NotFoundException
import com.github.butvinmitmo.divinationservice.mapper.InterpretationMapper
import com.github.butvinmitmo.divinationservice.mapper.SpreadMapper
import com.github.butvinmitmo.divinationservice.repository.InterpretationRepository
import com.github.butvinmitmo.divinationservice.repository.SpreadCardRepository
import com.github.butvinmitmo.divinationservice.repository.SpreadRepository
import com.github.butvinmitmo.divinationservice.service.DivinationService
import com.github.butvinmitmo.shared.dto.ArcanaTypeDto
import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.CreateInterpretationRequest
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.LayoutTypeDto
import com.github.butvinmitmo.shared.dto.UpdateInterpretationRequest
import com.github.butvinmitmo.shared.dto.UserDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
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
    private lateinit var userClient: UserClient

    @Mock
    private lateinit var tarotClient: TarotClient

    @Mock
    private lateinit var spreadMapper: SpreadMapper

    @Mock
    private lateinit var interpretationMapper: InterpretationMapper

    private lateinit var divinationService: DivinationService

    private val userId = UUID.randomUUID()
    private val spreadId = UUID.randomUUID()
    private val layoutTypeId = UUID.randomUUID()
    private val interpretationId = UUID.randomUUID()
    private val createdAt = Instant.now()

    private val testUser = UserDto(id = userId, username = "testuser", createdAt = createdAt)
    private val testLayoutType = LayoutTypeDto(id = layoutTypeId, name = "THREE_CARDS", cardsCount = 3)
    private val testArcanaType = ArcanaTypeDto(id = UUID.randomUUID(), name = "MAJOR")
    private val testCards =
        listOf(
            CardDto(id = UUID.randomUUID(), name = "The Fool", arcanaType = testArcanaType),
            CardDto(id = UUID.randomUUID(), name = "The Magician", arcanaType = testArcanaType),
            CardDto(id = UUID.randomUUID(), name = "The High Priestess", arcanaType = testArcanaType),
        )

    @BeforeEach
    fun setup() {
        divinationService =
            DivinationService(
                spreadRepository,
                spreadCardRepository,
                interpretationRepository,
                userClient,
                tarotClient,
                spreadMapper,
                interpretationMapper,
            )
    }

    @Test
    fun `createSpread should create new spread successfully`() {
        val request = CreateSpreadRequest(question = "Test question", authorId = userId, layoutTypeId = layoutTypeId)
        val savedSpread =
            TestEntityFactory.createSpread(
                id = spreadId,
                question = "Test question",
                layoutTypeId = layoutTypeId,
                authorId = userId,
                createdAt = createdAt,
            )

        whenever(userClient.getUserById(userId)).thenReturn(testUser)
        whenever(tarotClient.getLayoutTypeById(layoutTypeId)).thenReturn(testLayoutType)
        whenever(spreadRepository.save(any())).thenReturn(savedSpread)
        whenever(tarotClient.getRandomCards(3)).thenReturn(testCards)

        val result = divinationService.createSpread(request)

        assertNotNull(result)
        assertEquals(spreadId, result.id)
        verify(userClient).getUserById(userId)
        verify(tarotClient).getLayoutTypeById(layoutTypeId)
        verify(spreadRepository).save(any())
    }

    @Test
    fun `getSpreads should return paginated spreads`() {
        val spreads =
            listOf(
                TestEntityFactory.createSpread(id = UUID.randomUUID(), authorId = userId, layoutTypeId = layoutTypeId),
                TestEntityFactory.createSpread(id = UUID.randomUUID(), authorId = userId, layoutTypeId = layoutTypeId),
            )
        val pageable = PageRequest.of(0, 2)
        val page = PageImpl(spreads, pageable, 2)

        whenever(spreadRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(page)

        val result = divinationService.getSpreads(0, 2)

        assertNotNull(result)
        assertEquals(2, result.content.size)
    }

    @Test
    fun `getSpread should throw NotFoundException when spread not found`() {
        whenever(spreadRepository.findByIdWithCards(spreadId)).thenReturn(null)

        val exception =
            assertThrows<NotFoundException> {
                divinationService.getSpread(spreadId)
            }
        assertEquals("Spread not found", exception.message)
    }

    @Test
    fun `deleteSpread should delete spread when user is author`() {
        val spread = TestEntityFactory.createSpread(id = spreadId, authorId = userId, layoutTypeId = layoutTypeId)

        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.of(spread))

        divinationService.deleteSpread(spreadId, userId)

        verify(spreadRepository).deleteById(spreadId)
    }

    @Test
    fun `deleteSpread should throw ForbiddenException when user is not author`() {
        val otherUserId = UUID.randomUUID()
        val spread = TestEntityFactory.createSpread(id = spreadId, authorId = otherUserId, layoutTypeId = layoutTypeId)

        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.of(spread))

        val exception =
            assertThrows<ForbiddenException> {
                divinationService.deleteSpread(spreadId, userId)
            }
        assertEquals("You can only delete your own spreads", exception.message)

        verify(spreadRepository, never()).deleteById(any())
    }

    @Test
    fun `deleteSpread should throw NotFoundException when spread not found`() {
        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.empty())

        val exception =
            assertThrows<NotFoundException> {
                divinationService.deleteSpread(spreadId, userId)
            }
        assertEquals("Spread not found", exception.message)

        verify(spreadRepository, never()).deleteById(any())
    }

    @Test
    fun `addInterpretation should create new interpretation successfully`() {
        val spread = TestEntityFactory.createSpread(id = spreadId, authorId = userId, layoutTypeId = layoutTypeId)
        val request = CreateInterpretationRequest(text = "Test interpretation", authorId = userId)
        val savedInterpretation =
            TestEntityFactory.createInterpretation(
                id = interpretationId,
                text = "Test interpretation",
                authorId = userId,
                spread = spread,
                createdAt = createdAt,
            )

        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.of(spread))
        whenever(userClient.getUserById(userId)).thenReturn(testUser)
        whenever(interpretationRepository.existsByAuthorAndSpread(userId, spreadId)).thenReturn(false)
        whenever(interpretationRepository.save(any())).thenReturn(savedInterpretation)

        val result = divinationService.addInterpretation(spreadId, request)

        assertNotNull(result)
        assertEquals(interpretationId, result.id)
    }

    @Test
    fun `addInterpretation should throw ConflictException when user already has interpretation`() {
        val spread = TestEntityFactory.createSpread(id = spreadId, authorId = userId, layoutTypeId = layoutTypeId)
        val request = CreateInterpretationRequest(text = "Test interpretation", authorId = userId)

        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.of(spread))
        whenever(userClient.getUserById(userId)).thenReturn(testUser)
        whenever(interpretationRepository.existsByAuthorAndSpread(userId, spreadId)).thenReturn(true)

        val exception =
            assertThrows<ConflictException> {
                divinationService.addInterpretation(spreadId, request)
            }
        assertEquals("You already have an interpretation for this spread", exception.message)

        verify(interpretationRepository, never()).save(any())
    }

    @Test
    fun `updateInterpretation should update when user is author`() {
        val spread = TestEntityFactory.createSpread(id = spreadId, authorId = userId, layoutTypeId = layoutTypeId)
        val interpretation =
            TestEntityFactory.createInterpretation(
                id = interpretationId,
                text = "Original text",
                authorId = userId,
                spread = spread,
                createdAt = createdAt,
            )
        val request = UpdateInterpretationRequest(text = "Updated text", authorId = userId)

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.of(interpretation))
        whenever(interpretationRepository.save(any())).thenReturn(interpretation)

        divinationService.updateInterpretation(spreadId, interpretationId, userId, request)

        verify(interpretationRepository).save(any())
    }

    @Test
    fun `updateInterpretation should throw ForbiddenException when user is not author`() {
        val otherUserId = UUID.randomUUID()
        val spread = TestEntityFactory.createSpread(id = spreadId, authorId = userId, layoutTypeId = layoutTypeId)
        val interpretation =
            TestEntityFactory.createInterpretation(
                id = interpretationId,
                text = "Original text",
                authorId = otherUserId,
                spread = spread,
                createdAt = createdAt,
            )
        val request = UpdateInterpretationRequest(text = "Updated text", authorId = userId)

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.of(interpretation))

        val exception =
            assertThrows<ForbiddenException> {
                divinationService.updateInterpretation(spreadId, interpretationId, userId, request)
            }
        assertEquals("You can only edit your own interpretations", exception.message)

        verify(interpretationRepository, never()).save(any())
    }

    @Test
    fun `deleteInterpretation should delete when user is author`() {
        val spread = TestEntityFactory.createSpread(id = spreadId, authorId = userId, layoutTypeId = layoutTypeId)
        val interpretation =
            TestEntityFactory.createInterpretation(
                id = interpretationId,
                text = "Test",
                authorId = userId,
                spread = spread,
                createdAt = createdAt,
            )

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.of(interpretation))

        divinationService.deleteInterpretation(spreadId, interpretationId, userId)

        verify(interpretationRepository).deleteById(interpretationId)
    }

    @Test
    fun `deleteInterpretation should throw ForbiddenException when user is not author`() {
        val otherUserId = UUID.randomUUID()
        val spread = TestEntityFactory.createSpread(id = spreadId, authorId = userId, layoutTypeId = layoutTypeId)
        val interpretation =
            TestEntityFactory.createInterpretation(
                id = interpretationId,
                text = "Test",
                authorId = otherUserId,
                spread = spread,
                createdAt = createdAt,
            )

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.of(interpretation))

        val exception =
            assertThrows<ForbiddenException> {
                divinationService.deleteInterpretation(spreadId, interpretationId, userId)
            }
        assertEquals("You can only delete your own interpretations", exception.message)

        verify(interpretationRepository, never()).deleteById(any())
    }

    @Test
    fun `getInterpretation should return interpretation when found`() {
        val spread = TestEntityFactory.createSpread(id = spreadId, authorId = userId, layoutTypeId = layoutTypeId)
        val interpretation =
            TestEntityFactory.createInterpretation(
                id = interpretationId,
                text = "Test",
                authorId = userId,
                spread = spread,
                createdAt = createdAt,
            )

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.of(interpretation))

        divinationService.getInterpretation(spreadId, interpretationId)

        verify(interpretationMapper).toDto(interpretation)
    }

    @Test
    fun `getInterpretation should throw NotFoundException when interpretation not in spread`() {
        val otherSpreadId = UUID.randomUUID()
        val spread = TestEntityFactory.createSpread(id = spreadId, authorId = userId, layoutTypeId = layoutTypeId)
        val interpretation =
            TestEntityFactory.createInterpretation(
                id = interpretationId,
                text = "Test",
                authorId = userId,
                spread = spread,
                createdAt = createdAt,
            )

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Optional.of(interpretation))

        val exception =
            assertThrows<NotFoundException> {
                divinationService.getInterpretation(otherSpreadId, interpretationId)
            }
        assertEquals("Interpretation not found in this spread", exception.message)
    }

    @Test
    fun `getInterpretations should return paginated interpretations`() {
        val spread = TestEntityFactory.createSpread(id = spreadId, authorId = userId, layoutTypeId = layoutTypeId)
        val interpretations =
            listOf(
                TestEntityFactory.createInterpretation(id = UUID.randomUUID(), authorId = userId, spread = spread),
                TestEntityFactory.createInterpretation(id = UUID.randomUUID(), authorId = userId, spread = spread),
            )
        val pageable = PageRequest.of(0, 2)
        val page = PageImpl(interpretations, pageable, 2)

        whenever(spreadRepository.findById(spreadId)).thenReturn(Optional.of(spread))
        whenever(interpretationRepository.findBySpreadIdOrderByCreatedAtDesc(spreadId, pageable)).thenReturn(page)

        val result = divinationService.getInterpretations(spreadId, 0, 2)

        assertNotNull(result)
        assertEquals(2, result.content.size)
    }
}
