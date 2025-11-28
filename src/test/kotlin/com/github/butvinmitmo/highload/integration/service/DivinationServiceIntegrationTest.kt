package com.github.butvinmitmo.highload.integration.service

import com.github.butvinmitmo.highload.dto.CreateInterpretationRequest
import com.github.butvinmitmo.highload.dto.CreateSpreadRequest
import com.github.butvinmitmo.highload.dto.CreateUserRequest
import com.github.butvinmitmo.highload.dto.UpdateInterpretationRequest
import com.github.butvinmitmo.highload.exception.ConflictException
import com.github.butvinmitmo.highload.exception.ForbiddenException
import com.github.butvinmitmo.highload.exception.NotFoundException
import com.github.butvinmitmo.highload.integration.BaseIntegrationTest
import com.github.butvinmitmo.highload.repository.InterpretationRepository
import com.github.butvinmitmo.highload.repository.LayoutTypeRepository
import com.github.butvinmitmo.highload.repository.SpreadCardRepository
import com.github.butvinmitmo.highload.repository.SpreadRepository
import com.github.butvinmitmo.highload.repository.UserRepository
import com.github.butvinmitmo.highload.service.DivinationService
import com.github.butvinmitmo.highload.service.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class DivinationServiceIntegrationTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var divinationService: DivinationService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var spreadRepository: SpreadRepository

    @Autowired
    private lateinit var spreadCardRepository: SpreadCardRepository

    @Autowired
    private lateinit var interpretationRepository: InterpretationRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var layoutTypeRepository: LayoutTypeRepository

    private lateinit var userId: UUID
    private lateinit var layoutTypeId: UUID

    @BeforeEach
    fun setup() {
        val user = userService.createUser(CreateUserRequest(username = "testuser"))
        userId = user.id

        val layoutType = layoutTypeRepository.findByName("ONE_CARD")
        layoutTypeId = layoutType?.id ?: throw IllegalStateException("ONE_CARD layout not found")
    }

    // ==================== Spread Tests ====================

    @Test
    fun `should create spread successfully with random cards`() {
        val request =
            CreateSpreadRequest(
                question = "What will happen?",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val result = divinationService.createSpread(request)

        assertNotNull(result)
        assertNotNull(result.id)

        val saved = spreadRepository.findByIdWithCards(result.id)
        assertNotNull(saved)
        assertEquals("What will happen?", saved?.question)
        assertEquals(1, saved?.spreadCards?.size) // ONE_CARD layout has 1 card
        assertNotNull(saved?.createdAt)
    }

    @Test
    fun `should create spread with three cards layout`() {
        val threeCardsLayout = layoutTypeRepository.findByName("THREE_CARDS")
        assertNotNull(threeCardsLayout)

        val request =
            CreateSpreadRequest(
                question = "Past, Present, Future?",
                layoutTypeId = threeCardsLayout!!.id,
                authorId = userId,
            )

        val result = divinationService.createSpread(request)

        val saved = spreadRepository.findByIdWithCards(result.id)
        assertNotNull(saved)
        assertEquals(3, saved?.spreadCards?.size)
    }

    @Test
    fun `should create spread with cross layout`() {
        val crossLayout = layoutTypeRepository.findByName("CROSS")
        assertNotNull(crossLayout)

        val request =
            CreateSpreadRequest(
                question = "Complex situation?",
                layoutTypeId = crossLayout!!.id,
                authorId = userId,
            )

        val result = divinationService.createSpread(request)

        val saved = spreadRepository.findByIdWithCards(result.id)
        assertNotNull(saved)
        assertEquals(crossLayout.cardsCount, saved?.spreadCards?.size)
    }

    @Test
    fun `should throw NotFoundException when creating spread with non-existent user`() {
        val nonExistentUserId = UUID.randomUUID()
        val request =
            CreateSpreadRequest(
                question = "What will happen?",
                layoutTypeId = layoutTypeId,
                authorId = nonExistentUserId,
            )

        val exception =
            assertThrows<NotFoundException> {
                divinationService.createSpread(request)
            }
        assertEquals("User not found", exception.message)
    }

    @Test
    fun `should throw NotFoundException when creating spread with non-existent layout type`() {
        val nonExistentLayoutId = UUID.randomUUID()
        val request =
            CreateSpreadRequest(
                question = "What will happen?",
                layoutTypeId = nonExistentLayoutId,
                authorId = userId,
            )

        val exception =
            assertThrows<NotFoundException> {
                divinationService.createSpread(request)
            }
        assertEquals("Layout type not found", exception.message)
    }

    @Test
    fun `should get spread by id successfully`() {
        val createRequest =
            CreateSpreadRequest(
                question = "What will happen?",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val created = divinationService.createSpread(createRequest)

        val result = spreadRepository.findByIdWithCards(created.id)

        assertNotNull(result)
        assertEquals(created.id, result!!.id)
        assertEquals("What will happen?", result.question)
        assertNotNull(result.spreadCards)
        assertEquals(1, result.spreadCards.size)
    }

    @Test
    fun `should throw NotFoundException when getting non-existent spread`() {
        val nonExistentId = UUID.randomUUID()

        val result = spreadRepository.findById(nonExistentId)

        assertFalse(result.isPresent)
    }

    @Test
    fun `should get spread DTO without interpretations`() {
        val createRequest =
            CreateSpreadRequest(
                question = "Will I succeed?",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val created = divinationService.createSpread(createRequest)

        val result = divinationService.getSpread(created.id)

        assertNotNull(result)
        assertEquals(created.id, result.id)
        assertEquals("Will I succeed?", result.question)
        assertNotNull(result.cards)
        assertEquals(1, result.cards.size)
        assertNotNull(result.interpretations)
        assertEquals(0, result.interpretations.size)
        assertEquals(userId, result.author.id)
        assertEquals(layoutTypeId, result.layoutType.id)
    }

    @Test
    fun `should get spread DTO with single interpretation`() {
        val spreadRequest =
            CreateSpreadRequest(
                question = "What does the future hold?",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val createdSpread = divinationService.createSpread(spreadRequest)

        val interpretationRequest =
            CreateInterpretationRequest(
                text = "The cards suggest positive changes ahead",
                authorId = userId,
            )

        divinationService.addInterpretation(createdSpread.id, interpretationRequest)

        val result = divinationService.getSpread(createdSpread.id)

        assertNotNull(result)
        assertEquals(createdSpread.id, result.id)
        assertEquals("What does the future hold?", result.question)
        assertNotNull(result.cards)
        assertEquals(1, result.cards.size)
        assertNotNull(result.interpretations)
        assertEquals(1, result.interpretations.size)
        assertEquals("The cards suggest positive changes ahead", result.interpretations[0].text)
        assertEquals(userId, result.interpretations[0].author.id)
    }

    @Test
    fun `should get spread DTO with multiple interpretations`() {
        val user2Id =
            userService
                .createUser(CreateUserRequest(username = "reader2"))
                .id

        val spreadRequest =
            CreateSpreadRequest(
                question = "Career advice?",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val createdSpread = divinationService.createSpread(spreadRequest)

        divinationService.addInterpretation(
            createdSpread.id,
            CreateInterpretationRequest(
                text = "First interpretation from user1",
                authorId = userId,
            ),
        )

        divinationService.addInterpretation(
            createdSpread.id,
            CreateInterpretationRequest(
                text = "Second interpretation from user2",
                authorId = user2Id,
            ),
        )

        val result = divinationService.getSpread(createdSpread.id)

        assertNotNull(result)
        assertEquals(createdSpread.id, result.id)
        assertEquals("Career advice?", result.question)
        assertNotNull(result.cards)
        assertEquals(1, result.cards.size)
        assertNotNull(result.interpretations)
        assertEquals(2, result.interpretations.size)

        val interpretationTexts = result.interpretations.map { it.text }
        assertTrue(interpretationTexts.contains("First interpretation from user1"))
        assertTrue(interpretationTexts.contains("Second interpretation from user2"))
    }

    @Test
    fun `should throw NotFoundException when getting non-existent spread via service`() {
        val nonExistentId = UUID.randomUUID()

        val exception =
            assertThrows<NotFoundException> {
                divinationService.getSpread(nonExistentId)
            }

        assertEquals("Spread not found", exception.message)
    }

    @Test
    fun `should get paginated spreads count successfully`() {
        divinationService.createSpread(
            CreateSpreadRequest(
                question = "Question 1",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            ),
        )
        divinationService.createSpread(
            CreateSpreadRequest(
                question = "Question 2",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            ),
        )
        divinationService.createSpread(
            CreateSpreadRequest(
                question = "Question 3",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            ),
        )

        val allSpreads = spreadRepository.findAll()
        assertEquals(3, allSpreads.size)
    }

    @Test
    fun `should support scroll pagination`() {
        val createdIds =
            (1..5).map { i ->
                divinationService
                    .createSpread(
                        CreateSpreadRequest(
                            question = "Question $i",
                            layoutTypeId = layoutTypeId,
                            authorId = userId,
                        ),
                    ).id
            }

        val firstPage = spreadRepository.findLatestSpreads(2)
        assertEquals(2, firstPage.size)

        assertTrue(
            firstPage[0].createdAt >= firstPage[1].createdAt,
            "First page should be ordered by createdAt descending",
        )

        val cursorId = firstPage.last().id
        val secondPage = spreadRepository.findSpreadsAfterCursor(cursorId, 2)
        assertEquals(2, secondPage.size)

        assertTrue(
            secondPage[0].createdAt >= secondPage[1].createdAt,
            "Second page should be ordered by createdAt descending",
        )

        val page1Ids = firstPage.map { it.id }.toSet()
        val page2Ids = secondPage.map { it.id }.toSet()
        assertTrue(page1Ids.intersect(page2Ids).isEmpty(), "Pages should not overlap")

        val thirdPage = spreadRepository.findSpreadsAfterCursor(secondPage.last().id, 2)
        assertEquals(1, thirdPage.size)

        val allRetrievedIds = (firstPage + secondPage + thirdPage).map { it.id }.toSet()
        assertEquals(createdIds.toSet(), allRetrievedIds, "All created spreads should be retrievable")
    }

    @Test
    fun `should delete spread successfully when user is author`() {
        val createRequest =
            CreateSpreadRequest(
                question = "What will happen?",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val created = divinationService.createSpread(createRequest)

        divinationService.deleteSpread(created.id, userId)

        val deleted = spreadRepository.findById(created.id)
        assertTrue(deleted.isEmpty)
    }

    @Test
    fun `should throw ForbiddenException when deleting spread by non-author`() {
        val createRequest =
            CreateSpreadRequest(
                question = "What will happen?",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val created = divinationService.createSpread(createRequest)

        val otherUserId = UUID.randomUUID()

        val exception =
            assertThrows<ForbiddenException> {
                divinationService.deleteSpread(created.id, otherUserId)
            }
        assertEquals("You can only delete your own spreads", exception.message)
    }

    @Test
    fun `should throw NotFoundException when deleting non-existent spread`() {
        val nonExistentId = UUID.randomUUID()

        val exception =
            assertThrows<NotFoundException> {
                divinationService.deleteSpread(nonExistentId, userId)
            }
        assertEquals("Spread not found", exception.message)
    }

    @Test
    fun `should get spread entity successfully`() {
        val createRequest =
            CreateSpreadRequest(
                question = "What will happen?",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val created = divinationService.createSpread(createRequest)

        val result = divinationService.getSpreadEntity(created.id)

        assertNotNull(result)
        assertEquals(created.id, result.id)
        assertEquals("What will happen?", result.question)
    }

    @Test
    fun `should throw NotFoundException when getting non-existent spread entity`() {
        val nonExistentId = UUID.randomUUID()

        val exception =
            assertThrows<NotFoundException> {
                divinationService.getSpreadEntity(nonExistentId)
            }
        assertEquals("Spread not found", exception.message)
    }

    @Test
    fun `should order spreads by creation date descending`() {
        val createdIds =
            listOf("First", "Second", "Third").map { question ->
                divinationService
                    .createSpread(
                        CreateSpreadRequest(
                            question = question,
                            layoutTypeId = layoutTypeId,
                            authorId = userId,
                        ),
                    ).id
            }

        val spreads =
            spreadRepository.findAllOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest
                    .of(0, 10),
            )

        assertEquals(3, spreads.content.size)

        for (i in 0 until spreads.content.size - 1) {
            assertTrue(
                spreads.content[i].createdAt >= spreads.content[i + 1].createdAt,
                "Spreads should be ordered by createdAt descending",
            )
        }

        val retrievedIds = spreads.content.map { it.id }.toSet()
        assertEquals(createdIds.toSet(), retrievedIds, "All created spreads should be retrieved")
    }

    @Test
    fun `should create spread with null question`() {
        val request =
            CreateSpreadRequest(
                question = null,
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val result = divinationService.createSpread(request)

        val saved = spreadRepository.findById(result.id)
        assertTrue(saved.isPresent)
        assertEquals(null, saved.get().question)
    }

    @Test
    fun `should return empty list when no spreads exist`() {
        val spreads = divinationService.getSpreads(0, 10)

        assertNotNull(spreads)
        assertEquals(0, spreads.content.size)
        assertEquals(0, spreads.totalElements)
    }

    @Test
    fun `should return empty result for scroll when no spreads exist`() {
        val result = divinationService.getSpreadsByScroll(null, 10)

        assertNotNull(result)
        assertEquals(0, result.items.size)
        assertEquals(null, result.nextCursor)
    }

    @Test
    fun `should return ScrollResponse with nextCursor when more items exist`() {
        (1..5).forEach { i ->
            divinationService.createSpread(
                CreateSpreadRequest(
                    question = "Question $i",
                    layoutTypeId = layoutTypeId,
                    authorId = userId,
                ),
            )
        }

        val result = divinationService.getSpreadsByScroll(null, 2)

        assertNotNull(result)
        assertEquals(2, result.items.size)
        assertNotNull(result.nextCursor)
    }

    @Test
    fun `should return ScrollResponse without nextCursor when no more items exist`() {
        (1..2).forEach { i ->
            divinationService.createSpread(
                CreateSpreadRequest(
                    question = "Question $i",
                    layoutTypeId = layoutTypeId,
                    authorId = userId,
                ),
            )
        }

        val result = divinationService.getSpreadsByScroll(null, 5)

        assertNotNull(result)
        assertEquals(2, result.items.size)
        assertEquals(null, result.nextCursor)
    }

    @Test
    fun `spread cards should have correct position and reversed status`() {
        val request =
            CreateSpreadRequest(
                question = "Test positions",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val result = divinationService.createSpread(request)

        val saved = spreadRepository.findByIdWithCards(result.id)
        assertNotNull(saved)

        val spreadCards = saved!!.spreadCards
        assertEquals(1, spreadCards.size)

        assertEquals(1, spreadCards[0].positionInSpread)
        assertNotNull(spreadCards[0].isReversed)
    }

    // ==================== Interpretation Tests ====================

    @Test
    fun `should add interpretation successfully`() {
        val spreadId = createTestSpread()

        val request =
            CreateInterpretationRequest(
                text = "This card represents new beginnings",
                authorId = userId,
            )

        val result = divinationService.addInterpretation(spreadId, request)

        assertNotNull(result)
        assertNotNull(result.id)

        val saved = interpretationRepository.findById(result.id)
        assertTrue(saved.isPresent)
        assertEquals("This card represents new beginnings", saved.get().text)
        assertEquals(userId, saved.get().author.id)
        assertNotNull(saved.get().createdAt)
    }

    @Test
    fun `should throw ConflictException when user adds duplicate interpretation`() {
        val spreadId = createTestSpread()

        val request =
            CreateInterpretationRequest(
                text = "First interpretation",
                authorId = userId,
            )

        divinationService.addInterpretation(spreadId, request)

        val duplicateRequest =
            CreateInterpretationRequest(
                text = "Second interpretation",
                authorId = userId,
            )

        val exception =
            assertThrows<ConflictException> {
                divinationService.addInterpretation(spreadId, duplicateRequest)
            }
        assertEquals("You already have an interpretation for this spread", exception.message)
    }

    @Test
    fun `should throw NotFoundException when adding interpretation to non-existent spread`() {
        val nonExistentSpreadId = UUID.randomUUID()
        val request =
            CreateInterpretationRequest(
                text = "This card represents...",
                authorId = userId,
            )

        val exception =
            assertThrows<NotFoundException> {
                divinationService.addInterpretation(nonExistentSpreadId, request)
            }
        assertEquals("Spread not found", exception.message)
    }

    @Test
    fun `should throw NotFoundException when adding interpretation by non-existent user`() {
        val spreadId = createTestSpread()

        val nonExistentUserId = UUID.randomUUID()
        val request =
            CreateInterpretationRequest(
                text = "This card represents...",
                authorId = nonExistentUserId,
            )

        val exception =
            assertThrows<NotFoundException> {
                divinationService.addInterpretation(spreadId, request)
            }
        assertEquals("User not found", exception.message)
    }

    @Test
    fun `should allow different users to add interpretations to same spread`() {
        val spreadId = createTestSpread()

        val user2 = userService.createUser(CreateUserRequest(username = "user2"))

        val request1 =
            CreateInterpretationRequest(
                text = "User 1 interpretation",
                authorId = userId,
            )
        val result1 = divinationService.addInterpretation(spreadId, request1)

        val request2 =
            CreateInterpretationRequest(
                text = "User 2 interpretation",
                authorId = user2.id,
            )
        val result2 = divinationService.addInterpretation(spreadId, request2)

        assertNotNull(result1.id)
        assertNotNull(result2.id)

        val allInterpretations = interpretationRepository.findAll()
        assertEquals(2, allInterpretations.size)
    }

    @Test
    fun `should update interpretation successfully`() {
        val spreadId = createTestSpread()

        val createRequest =
            CreateInterpretationRequest(
                text = "Original text",
                authorId = userId,
            )
        val created = divinationService.addInterpretation(spreadId, createRequest)

        val updateRequest = UpdateInterpretationRequest(text = "Updated text", authorId = userId)
        val result = divinationService.updateInterpretation(spreadId, created.id, userId, updateRequest)

        assertNotNull(result)
        assertEquals(created.id, result.id)
        assertEquals("Updated text", result.text)

        val updated = interpretationRepository.findById(created.id)
        assertTrue(updated.isPresent)
        assertEquals("Updated text", updated.get().text)
    }

    @Test
    fun `should throw NotFoundException when updating non-existent interpretation`() {
        val spreadId = createTestSpread()

        val nonExistentId = UUID.randomUUID()
        val updateRequest = UpdateInterpretationRequest(text = "Updated text", authorId = userId)

        val exception =
            assertThrows<NotFoundException> {
                divinationService.updateInterpretation(spreadId, nonExistentId, userId, updateRequest)
            }
        assertEquals("Interpretation not found", exception.message)
    }

    @Test
    fun `should throw ForbiddenException when updating another user's interpretation`() {
        val spreadId = createTestSpread()

        val createRequest =
            CreateInterpretationRequest(
                text = "Original text",
                authorId = userId,
            )
        val created = divinationService.addInterpretation(spreadId, createRequest)

        val otherUser = userService.createUser(CreateUserRequest(username = "otheruser"))
        val updateRequest = UpdateInterpretationRequest(text = "Hacked text", authorId = otherUser.id)

        val exception =
            assertThrows<ForbiddenException> {
                divinationService.updateInterpretation(spreadId, created.id, otherUser.id, updateRequest)
            }
        assertEquals("You can only edit your own interpretations", exception.message)
    }

    @Test
    fun `should delete interpretation successfully`() {
        val spreadId = createTestSpread()

        val createRequest =
            CreateInterpretationRequest(
                text = "Text to delete",
                authorId = userId,
            )
        val created = divinationService.addInterpretation(spreadId, createRequest)

        divinationService.deleteInterpretation(spreadId, created.id, userId)

        val deleted = interpretationRepository.findById(created.id)
        assertTrue(deleted.isEmpty)
    }

    @Test
    fun `should throw NotFoundException when deleting non-existent interpretation`() {
        val spreadId = createTestSpread()

        val nonExistentId = UUID.randomUUID()

        val exception =
            assertThrows<NotFoundException> {
                divinationService.deleteInterpretation(spreadId, nonExistentId, userId)
            }
        assertEquals("Interpretation not found", exception.message)
    }

    @Test
    fun `should throw ForbiddenException when deleting another user's interpretation`() {
        val spreadId = createTestSpread()

        val createRequest =
            CreateInterpretationRequest(
                text = "Original text",
                authorId = userId,
            )
        val created = divinationService.addInterpretation(spreadId, createRequest)

        val otherUser = userService.createUser(CreateUserRequest(username = "otheruser"))

        val exception =
            assertThrows<ForbiddenException> {
                divinationService.deleteInterpretation(spreadId, created.id, otherUser.id)
            }
        assertEquals("You can only delete your own interpretations", exception.message)
    }

    @Test
    fun `should get interpretation by id successfully`() {
        val spreadId = createTestSpread()

        val createRequest =
            CreateInterpretationRequest(
                text = "Test interpretation",
                authorId = userId,
            )
        val created = divinationService.addInterpretation(spreadId, createRequest)

        val result = divinationService.getInterpretation(spreadId, created.id)

        assertNotNull(result)
        assertEquals(created.id, result.id)
        assertEquals("Test interpretation", result.text)
        assertEquals(userId, result.author.id)
        assertEquals(spreadId, result.spreadId)
    }

    @Test
    fun `should throw NotFoundException when getting non-existent interpretation`() {
        val spreadId = createTestSpread()

        val nonExistentId = UUID.randomUUID()

        val exception =
            assertThrows<NotFoundException> {
                divinationService.getInterpretation(spreadId, nonExistentId)
            }
        assertEquals("Interpretation not found", exception.message)
    }

    @Test
    fun `should throw NotFoundException when getting interpretation from wrong spread`() {
        val spreadId = createTestSpread()

        val createRequest =
            CreateInterpretationRequest(
                text = "Test interpretation",
                authorId = userId,
            )
        val created = divinationService.addInterpretation(spreadId, createRequest)

        val differentSpreadId =
            divinationService
                .createSpread(
                    CreateSpreadRequest(
                        question = "Different question?",
                        layoutTypeId = layoutTypeId,
                        authorId = userId,
                    ),
                ).id

        val exception =
            assertThrows<NotFoundException> {
                divinationService.getInterpretation(differentSpreadId, created.id)
            }
        assertEquals("Interpretation not found in this spread", exception.message)
    }

    @Test
    fun `should get all interpretations for a spread`() {
        val spreadId = createTestSpread()

        val user2 = userService.createUser(CreateUserRequest(username = "user2"))

        val request1 =
            CreateInterpretationRequest(
                text = "First interpretation",
                authorId = userId,
            )
        divinationService.addInterpretation(spreadId, request1)

        val request2 =
            CreateInterpretationRequest(
                text = "Second interpretation",
                authorId = user2.id,
            )
        divinationService.addInterpretation(spreadId, request2)

        val result = divinationService.getInterpretations(spreadId, 0, 20)

        assertNotNull(result)
        assertEquals(2, result.content.size)
        assertTrue(result.content.any { it.text == "First interpretation" })
        assertTrue(result.content.any { it.text == "Second interpretation" })
    }

    @Test
    fun `should return empty list when spread has no interpretations`() {
        val spreadId = createTestSpread()

        val result = divinationService.getInterpretations(spreadId, 0, 20)

        assertNotNull(result)
        assertEquals(0, result.content.size)
    }

    @Test
    fun `should return interpretations ordered by creation date descending`() {
        val spreadId = createTestSpread()

        val user2 = userService.createUser(CreateUserRequest(username = "user2"))
        val user3 = userService.createUser(CreateUserRequest(username = "user3"))

        val interp1 =
            divinationService.addInterpretation(
                spreadId,
                CreateInterpretationRequest(text = "First interpretation", authorId = userId),
            )
        val interp2 =
            divinationService.addInterpretation(
                spreadId,
                CreateInterpretationRequest(text = "Second interpretation", authorId = user2.id),
            )
        val interp3 =
            divinationService.addInterpretation(
                spreadId,
                CreateInterpretationRequest(text = "Third interpretation", authorId = user3.id),
            )

        val result = divinationService.getInterpretations(spreadId, 0, 20)

        assertEquals(3, result.content.size)

        for (i in 0 until result.content.size - 1) {
            assertTrue(
                result.content[i].createdAt >= result.content[i + 1].createdAt,
                "Interpretations should be ordered by createdAt descending",
            )
        }

        val resultIds = result.content.map { it.id }.toSet()
        val createdIds = setOf(interp1.id, interp2.id, interp3.id)
        assertEquals(createdIds, resultIds, "All created interpretations should be retrieved")
    }

    @Test
    fun `should throw NotFoundException when getting interpretations for non-existent spread`() {
        val nonExistentSpreadId = UUID.randomUUID()

        val exception =
            assertThrows<NotFoundException> {
                divinationService.getInterpretations(nonExistentSpreadId, 0, 20)
            }
        assertEquals("Spread not found", exception.message)
    }

    // ==================== Helper Methods ====================

    private fun createTestSpread(): UUID =
        divinationService
            .createSpread(
                CreateSpreadRequest(
                    question = "What will happen?",
                    layoutTypeId = layoutTypeId,
                    authorId = userId,
                ),
            ).id
}
