package com.github.butvinmitmo.divinationservice.unit.mapper

import com.github.butvinmitmo.divinationservice.entity.Interpretation
import com.github.butvinmitmo.divinationservice.entity.Spread
import com.github.butvinmitmo.divinationservice.entity.SpreadCard
import com.github.butvinmitmo.divinationservice.mapper.SpreadMapper
import com.github.butvinmitmo.shared.client.TarotServiceClient
import com.github.butvinmitmo.shared.client.UserServiceClient
import com.github.butvinmitmo.shared.dto.ArcanaTypeDto
import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.LayoutTypeDto
import com.github.butvinmitmo.shared.dto.UserDto
import feign.FeignException
import feign.Request
import feign.RequestTemplate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.http.ResponseEntity
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SpreadMapperTest {
    @Mock
    private lateinit var userServiceClient: UserServiceClient

    @Mock
    private lateinit var tarotServiceClient: TarotServiceClient

    private lateinit var spreadMapper: SpreadMapper

    private val userId = UUID.randomUUID()
    private val spreadId = UUID.randomUUID()
    private val layoutTypeId = UUID.randomUUID()
    private val interpretationId = UUID.randomUUID()
    private val cardId = UUID.randomUUID()
    private val createdAt = Instant.now()

    private val testUser = UserDto(id = userId, username = "testuser", createdAt = createdAt)
    private val testLayoutType = LayoutTypeDto(id = layoutTypeId, name = "THREE_CARDS", cardsCount = 3)
    private val testArcanaType = ArcanaTypeDto(id = UUID.randomUUID(), name = "MAJOR")
    private val testCard = CardDto(id = cardId, name = "The Fool", arcanaType = testArcanaType)

    @BeforeEach
    fun setup() {
        spreadMapper = SpreadMapper(userServiceClient, tarotServiceClient)
    }

    @Test
    fun `toSummaryDto should return placeholder when user not found`() {
        val spread = createTestSpread()
        whenever(userServiceClient.getUserById(userId)).thenThrow(createNotFoundException())
        whenever(tarotServiceClient.getLayoutTypeById(layoutTypeId))
            .thenReturn(ResponseEntity.ok(testLayoutType))

        val result = spreadMapper.toSummaryDto(spread, 5)

        assertEquals("[Deleted User]", result.authorUsername)
        assertEquals("THREE_CARDS", result.layoutTypeName)
        assertEquals(5, result.interpretationsCount)
    }

    @Test
    fun `toSummaryDto should return placeholder when layout type not found`() {
        val spread = createTestSpread()
        whenever(userServiceClient.getUserById(userId))
            .thenReturn(ResponseEntity.ok(testUser))
        whenever(tarotServiceClient.getLayoutTypeById(layoutTypeId)).thenThrow(createNotFoundException())

        val result = spreadMapper.toSummaryDto(spread, 5)

        assertEquals("testuser", result.authorUsername)
        assertEquals("[Deleted Layout]", result.layoutTypeName)
        assertEquals(0, result.cardsCount)
    }

    @Test
    fun `toSummaryDto should return both placeholders when user and layout type not found`() {
        val spread = createTestSpread()
        whenever(userServiceClient.getUserById(userId)).thenThrow(createNotFoundException())
        whenever(tarotServiceClient.getLayoutTypeById(layoutTypeId)).thenThrow(createNotFoundException())

        val result = spreadMapper.toSummaryDto(spread, 5)

        assertEquals("[Deleted User]", result.authorUsername)
        assertEquals("[Deleted Layout]", result.layoutTypeName)
        assertEquals(0, result.cardsCount)
    }

    @Test
    fun `toDto should return placeholder for deleted author`() {
        val spread = createTestSpread()
        val spreadCards = listOf(createTestSpreadCard())
        val interpretations = emptyList<Interpretation>()
        val cardCache = mapOf(cardId to testCard)

        whenever(userServiceClient.getUserById(userId)).thenThrow(createNotFoundException())
        whenever(tarotServiceClient.getLayoutTypeById(layoutTypeId))
            .thenReturn(ResponseEntity.ok(testLayoutType))

        val result = spreadMapper.toDto(spread, spreadCards, interpretations, cardCache)

        assertEquals("[Deleted User]", result.author.username)
        assertEquals(Instant.EPOCH, result.author.createdAt)
        assertEquals(userId, result.author.id)
    }

    @Test
    fun `toDto should return placeholder for deleted interpretation author`() {
        val spread = createTestSpread()
        val spreadCards = listOf(createTestSpreadCard())
        val deletedUserId = UUID.randomUUID()
        val interpretations = listOf(createTestInterpretation(deletedUserId))
        val cardCache = mapOf(cardId to testCard)

        whenever(userServiceClient.getUserById(userId))
            .thenReturn(ResponseEntity.ok(testUser))
        whenever(userServiceClient.getUserById(deletedUserId)).thenThrow(createNotFoundException())
        whenever(tarotServiceClient.getLayoutTypeById(layoutTypeId))
            .thenReturn(ResponseEntity.ok(testLayoutType))

        val result = spreadMapper.toDto(spread, spreadCards, interpretations, cardCache)

        assertEquals("testuser", result.author.username)
        assertEquals(1, result.interpretations.size)
        assertEquals("[Deleted User]", result.interpretations[0].author.username)
    }

    private fun createTestSpread(): Spread =
        Spread(
            id = spreadId,
            question = "Test question?",
            layoutTypeId = layoutTypeId,
            authorId = userId,
            createdAt = createdAt,
        )

    private fun createTestSpreadCard(): SpreadCard =
        SpreadCard(
            id = UUID.randomUUID(),
            spreadId = spreadId,
            cardId = cardId,
            positionInSpread = 1,
            isReversed = false,
        )

    private fun createTestInterpretation(authorId: UUID): Interpretation =
        Interpretation(
            id = interpretationId,
            text = "Test interpretation",
            authorId = authorId,
            spreadId = spreadId,
            createdAt = createdAt,
        )

    private fun createNotFoundException(): FeignException.NotFound {
        val request =
            Request.create(
                Request.HttpMethod.GET,
                "/test",
                emptyMap(),
                null,
                RequestTemplate(),
            )
        return FeignException.NotFound("Not Found", request, null, null)
    }
}
