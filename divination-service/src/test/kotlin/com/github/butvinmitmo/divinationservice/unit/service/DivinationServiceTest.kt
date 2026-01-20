package com.github.butvinmitmo.divinationservice.unit.service

import com.github.butvinmitmo.divinationservice.TestEntityFactory
import com.github.butvinmitmo.divinationservice.application.interfaces.provider.CardProvider
import com.github.butvinmitmo.divinationservice.application.interfaces.provider.CurrentUserProvider
import com.github.butvinmitmo.divinationservice.application.interfaces.provider.UserProvider
import com.github.butvinmitmo.divinationservice.application.interfaces.repository.InterpretationRepository
import com.github.butvinmitmo.divinationservice.application.interfaces.repository.SpreadCardRepository
import com.github.butvinmitmo.divinationservice.application.interfaces.repository.SpreadRepository
import com.github.butvinmitmo.divinationservice.application.service.DivinationService
import com.github.butvinmitmo.divinationservice.exception.ConflictException
import com.github.butvinmitmo.divinationservice.exception.ForbiddenException
import com.github.butvinmitmo.divinationservice.exception.NotFoundException
import com.github.butvinmitmo.shared.dto.ArcanaTypeDto
import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.LayoutTypeDto
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
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
    private lateinit var userProvider: UserProvider

    @Mock
    private lateinit var cardProvider: CardProvider

    @Mock
    private lateinit var currentUserProvider: CurrentUserProvider

    private lateinit var divinationService: DivinationService

    private val userId = UUID.randomUUID()
    private val spreadId = UUID.randomUUID()
    private val layoutTypeId = UUID.randomUUID()
    private val interpretationId = UUID.randomUUID()
    private val createdAt = Instant.now()

    private val testUser = UserDto(id = userId, username = "testuser", role = "USER", createdAt = createdAt)
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
                userProvider,
                cardProvider,
                currentUserProvider,
            )
    }

    @Test
    fun `createSpread should create new spread successfully`() {
        val savedSpread =
            TestEntityFactory.createSpread(
                id = spreadId,
                question = "Test question",
                layoutTypeId = layoutTypeId,
                authorId = userId,
                createdAt = createdAt,
            )

        whenever(currentUserProvider.getCurrentUserId()).thenReturn(Mono.just(userId))
        whenever(currentUserProvider.getCurrentRole()).thenReturn(Mono.just("USER"))
        whenever(userProvider.getUserById(userId, "USER", userId)).thenReturn(Mono.just(testUser))
        whenever(cardProvider.getLayoutTypeById(userId, "USER", layoutTypeId)).thenReturn(Mono.just(testLayoutType))
        whenever(spreadRepository.save(any())).thenReturn(Mono.just(savedSpread))
        whenever(spreadCardRepository.save(any())).thenReturn(
            Mono.just(TestEntityFactory.createSpreadCard(spreadId = spreadId)),
        )
        whenever(cardProvider.getRandomCards(userId, "USER", 3)).thenReturn(Mono.just(testCards))

        val result = divinationService.createSpread("Test question", layoutTypeId).block()

        assertNotNull(result)
        assertEquals(spreadId, result!!.id)
        verify(userProvider).getUserById(userId, "USER", userId)
        verify(cardProvider).getLayoutTypeById(userId, "USER", layoutTypeId)
        verify(spreadRepository).save(any())
    }

    @Test
    fun `getSpreads should return paginated spreads`() {
        val spreads =
            listOf(
                TestEntityFactory.createSpread(id = UUID.randomUUID(), layoutTypeId = layoutTypeId),
                TestEntityFactory.createSpread(id = UUID.randomUUID(), layoutTypeId = layoutTypeId),
            )

        whenever(spreadRepository.count()).thenReturn(Mono.just(2L))
        whenever(spreadRepository.findAllOrderByCreatedAtDesc(0L, 2)).thenReturn(Flux.fromIterable(spreads))
        whenever(interpretationRepository.countBySpreadIds(any())).thenReturn(Flux.empty())
        whenever(userProvider.getSystemUser(any())).thenReturn(Mono.just(testUser))
        whenever(cardProvider.getSystemLayoutType(any())).thenReturn(Mono.just(testLayoutType))

        val result = divinationService.getSpreads(0, 2).block()

        assertNotNull(result)
        assertEquals(2, result!!.content.size)
    }

    @Test
    fun `getSpread should throw NotFoundException when spread not found`() {
        whenever(spreadRepository.findById(spreadId)).thenReturn(Mono.empty())

        val exception =
            assertThrows<NotFoundException> {
                divinationService.getSpread(spreadId).block()
            }
        assertEquals("Spread not found", exception.message)
    }

    @Test
    fun `deleteSpread should delete spread when user is author`() {
        val spread = TestEntityFactory.createSpread(id = spreadId, layoutTypeId = layoutTypeId, authorId = userId)

        whenever(spreadRepository.findById(spreadId)).thenReturn(Mono.just(spread))
        whenever(currentUserProvider.canModify(userId)).thenReturn(Mono.just(true))
        whenever(spreadRepository.deleteById(spreadId)).thenReturn(Mono.empty())

        divinationService.deleteSpread(spreadId).block()

        verify(spreadRepository).deleteById(spreadId)
    }

    @Test
    fun `deleteSpread should throw ForbiddenException when user is not author`() {
        val otherUserId = UUID.randomUUID()
        val spread = TestEntityFactory.createSpread(id = spreadId, layoutTypeId = layoutTypeId, authorId = otherUserId)

        whenever(spreadRepository.findById(spreadId)).thenReturn(Mono.just(spread))
        whenever(currentUserProvider.canModify(otherUserId)).thenReturn(Mono.just(false))

        val exception =
            assertThrows<ForbiddenException> {
                divinationService.deleteSpread(spreadId).block()
            }
        assertEquals("You can only delete your own spreads", exception.message)

        verify(spreadRepository, never()).deleteById(any<UUID>())
    }

    @Test
    fun `deleteSpread should throw NotFoundException when spread not found`() {
        whenever(spreadRepository.findById(spreadId)).thenReturn(Mono.empty())

        val exception =
            assertThrows<NotFoundException> {
                divinationService.deleteSpread(spreadId).block()
            }
        assertEquals("Spread not found", exception.message)

        verify(spreadRepository, never()).deleteById(any<UUID>())
    }

    @Test
    fun `addInterpretation should create new interpretation successfully`() {
        val spread = TestEntityFactory.createSpread(id = spreadId, layoutTypeId = layoutTypeId)
        val savedInterpretation =
            TestEntityFactory.createInterpretation(
                id = interpretationId,
                text = "Test interpretation",
                authorId = userId,
                spreadId = spreadId,
                createdAt = createdAt,
            )

        whenever(currentUserProvider.getCurrentUserId()).thenReturn(Mono.just(userId))
        whenever(currentUserProvider.getCurrentRole()).thenReturn(Mono.just("USER"))
        whenever(spreadRepository.findById(spreadId)).thenReturn(Mono.just(spread))
        whenever(userProvider.getUserById(userId, "USER", userId)).thenReturn(Mono.just(testUser))
        whenever(interpretationRepository.existsByAuthorAndSpread(userId, spreadId)).thenReturn(Mono.just(false))
        whenever(interpretationRepository.save(any())).thenReturn(Mono.just(savedInterpretation))

        val result = divinationService.addInterpretation(spreadId, "Test interpretation").block()

        assertNotNull(result)
        assertEquals(interpretationId, result!!.id)
    }

    @Test
    fun `addInterpretation should throw ConflictException when user already has interpretation`() {
        val spread = TestEntityFactory.createSpread(id = spreadId, layoutTypeId = layoutTypeId)

        whenever(currentUserProvider.getCurrentUserId()).thenReturn(Mono.just(userId))
        whenever(currentUserProvider.getCurrentRole()).thenReturn(Mono.just("USER"))
        whenever(spreadRepository.findById(spreadId)).thenReturn(Mono.just(spread))
        whenever(userProvider.getUserById(userId, "USER", userId)).thenReturn(Mono.just(testUser))
        whenever(interpretationRepository.existsByAuthorAndSpread(userId, spreadId)).thenReturn(Mono.just(true))

        val exception =
            assertThrows<ConflictException> {
                divinationService.addInterpretation(spreadId, "Test interpretation").block()
            }
        assertEquals("You already have an interpretation for this spread", exception.message)

        verify(interpretationRepository, never()).save(any())
    }

    @Test
    fun `updateInterpretation should update when user is author`() {
        val interpretation =
            TestEntityFactory.createInterpretation(
                id = interpretationId,
                text = "Original text",
                authorId = userId,
                spreadId = spreadId,
                createdAt = createdAt,
            )

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Mono.just(interpretation))
        whenever(currentUserProvider.canModify(userId)).thenReturn(Mono.just(true))
        whenever(interpretationRepository.save(any())).thenReturn(Mono.just(interpretation.copy(text = "Updated text")))
        whenever(userProvider.getSystemUser(userId)).thenReturn(Mono.just(testUser))

        divinationService.updateInterpretation(spreadId, interpretationId, "Updated text").block()

        verify(interpretationRepository).save(any())
    }

    @Test
    fun `updateInterpretation should throw ForbiddenException when user is not author`() {
        val otherUserId = UUID.randomUUID()
        val interpretation =
            TestEntityFactory.createInterpretation(
                id = interpretationId,
                text = "Original text",
                authorId = otherUserId,
                spreadId = spreadId,
                createdAt = createdAt,
            )

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Mono.just(interpretation))
        whenever(currentUserProvider.canModify(otherUserId)).thenReturn(Mono.just(false))

        val exception =
            assertThrows<ForbiddenException> {
                divinationService.updateInterpretation(spreadId, interpretationId, "Updated text").block()
            }
        assertEquals("You can only edit your own interpretations", exception.message)

        verify(interpretationRepository, never()).save(any())
    }

    @Test
    fun `deleteInterpretation should delete when user is author`() {
        val interpretation =
            TestEntityFactory.createInterpretation(
                id = interpretationId,
                text = "Test",
                authorId = userId,
                spreadId = spreadId,
                createdAt = createdAt,
            )

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Mono.just(interpretation))
        whenever(currentUserProvider.canModify(userId)).thenReturn(Mono.just(true))
        whenever(interpretationRepository.deleteById(interpretationId)).thenReturn(Mono.empty())

        divinationService.deleteInterpretation(spreadId, interpretationId).block()

        verify(interpretationRepository).deleteById(interpretationId)
    }

    @Test
    fun `deleteInterpretation should throw ForbiddenException when user is not author`() {
        val otherUserId = UUID.randomUUID()
        val interpretation =
            TestEntityFactory.createInterpretation(
                id = interpretationId,
                text = "Test",
                authorId = otherUserId,
                spreadId = spreadId,
                createdAt = createdAt,
            )

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Mono.just(interpretation))
        whenever(currentUserProvider.canModify(otherUserId)).thenReturn(Mono.just(false))

        val exception =
            assertThrows<ForbiddenException> {
                divinationService.deleteInterpretation(spreadId, interpretationId).block()
            }
        assertEquals("You can only delete your own interpretations", exception.message)

        verify(interpretationRepository, never()).deleteById(any<UUID>())
    }

    @Test
    fun `getInterpretation should return interpretation when found`() {
        val interpretation =
            TestEntityFactory.createInterpretation(
                id = interpretationId,
                text = "Test",
                authorId = userId,
                spreadId = spreadId,
                createdAt = createdAt,
            )

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Mono.just(interpretation))
        whenever(userProvider.getSystemUser(userId)).thenReturn(Mono.just(testUser))

        val result = divinationService.getInterpretation(spreadId, interpretationId).block()

        assertNotNull(result)
        assertEquals(interpretationId, result!!.id)
    }

    @Test
    fun `getInterpretation should throw NotFoundException when interpretation not in spread`() {
        val otherSpreadId = UUID.randomUUID()
        val interpretation =
            TestEntityFactory.createInterpretation(
                id = interpretationId,
                text = "Test",
                authorId = userId,
                spreadId = spreadId,
                createdAt = createdAt,
            )

        whenever(interpretationRepository.findById(interpretationId)).thenReturn(Mono.just(interpretation))

        val exception =
            assertThrows<NotFoundException> {
                divinationService.getInterpretation(otherSpreadId, interpretationId).block()
            }
        assertEquals("Interpretation not found in this spread", exception.message)
    }

    @Test
    fun `getInterpretations should return paginated interpretations`() {
        val spread = TestEntityFactory.createSpread(id = spreadId, layoutTypeId = layoutTypeId)
        val interpretations =
            listOf(
                TestEntityFactory.createInterpretation(id = UUID.randomUUID(), spreadId = spreadId),
                TestEntityFactory.createInterpretation(id = UUID.randomUUID(), spreadId = spreadId),
            )

        whenever(spreadRepository.findById(spreadId)).thenReturn(Mono.just(spread))
        whenever(interpretationRepository.findBySpreadIdOrderByCreatedAtDesc(spreadId, 0L, 2))
            .thenReturn(Flux.fromIterable(interpretations))
        whenever(interpretationRepository.countBySpreadId(spreadId)).thenReturn(Mono.just(2L))
        whenever(userProvider.getSystemUser(any())).thenReturn(Mono.just(testUser))

        val result = divinationService.getInterpretations(spreadId, 0, 2).block()

        assertNotNull(result)
        assertEquals(2, result!!.content.size)
    }

    @Test
    fun `deleteUserData should delete interpretations and spreads for user`() {
        whenever(interpretationRepository.deleteByAuthorId(userId)).thenReturn(Mono.empty())
        whenever(spreadRepository.deleteByAuthorId(userId)).thenReturn(Mono.empty())

        divinationService.deleteUserData(userId).block()

        verify(interpretationRepository).deleteByAuthorId(userId)
        verify(spreadRepository).deleteByAuthorId(userId)
    }

    @Test
    fun `deleteUserData should complete successfully even if user has no data`() {
        whenever(interpretationRepository.deleteByAuthorId(userId)).thenReturn(Mono.empty())
        whenever(spreadRepository.deleteByAuthorId(userId)).thenReturn(Mono.empty())

        // Should complete without exception
        divinationService.deleteUserData(userId).block()

        verify(interpretationRepository).deleteByAuthorId(userId)
        verify(spreadRepository).deleteByAuthorId(userId)
    }
}
