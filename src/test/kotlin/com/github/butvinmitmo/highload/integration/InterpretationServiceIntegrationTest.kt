package com.github.butvinmitmo.highload.integration

import com.github.butvinmitmo.highload.dto.CreateInterpretationRequest
import com.github.butvinmitmo.highload.dto.CreateSpreadRequest
import com.github.butvinmitmo.highload.dto.CreateUserRequest
import com.github.butvinmitmo.highload.dto.UpdateInterpretationRequest
import com.github.butvinmitmo.highload.exception.ConflictException
import com.github.butvinmitmo.highload.exception.ForbiddenException
import com.github.butvinmitmo.highload.exception.NotFoundException
import com.github.butvinmitmo.highload.repository.InterpretationRepository
import com.github.butvinmitmo.highload.repository.LayoutTypeRepository
import com.github.butvinmitmo.highload.repository.SpreadCardRepository
import com.github.butvinmitmo.highload.repository.SpreadRepository
import com.github.butvinmitmo.highload.repository.UserRepository
import com.github.butvinmitmo.highload.service.InterpretationService
import com.github.butvinmitmo.highload.service.SpreadService
import com.github.butvinmitmo.highload.service.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class InterpretationServiceIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var interpretationService: InterpretationService

    @Autowired
    private lateinit var spreadService: SpreadService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var interpretationRepository: InterpretationRepository

    @Autowired
    private lateinit var spreadRepository: SpreadRepository

    @Autowired
    private lateinit var spreadCardRepository: SpreadCardRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var layoutTypeRepository: LayoutTypeRepository

    private lateinit var userId: UUID
    private lateinit var spreadId: UUID
    private lateinit var layoutTypeId: UUID

    @BeforeEach
    fun setup() {
        val user = userService.createUser(CreateUserRequest(username = "testuser"))
        userId = user.id

        val layoutType = layoutTypeRepository.findByName("ONE_CARD")
        layoutTypeId = layoutType?.id ?: throw IllegalStateException("ONE_CARD layout not found")

        val spread =
            spreadService.createSpread(
                CreateSpreadRequest(
                    question = "What will happen?",
                    layoutTypeId = layoutTypeId,
                    authorId = userId,
                ),
            )
        spreadId = spread.id
    }

    @Test
    fun `should add interpretation successfully`() {
        val request =
            CreateInterpretationRequest(
                text = "This card represents new beginnings",
                authorId = userId,
            )

        val result = interpretationService.addInterpretation(spreadId, request)

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
        val request =
            CreateInterpretationRequest(
                text = "First interpretation",
                authorId = userId,
            )

        interpretationService.addInterpretation(spreadId, request)

        val duplicateRequest =
            CreateInterpretationRequest(
                text = "Second interpretation",
                authorId = userId,
            )

        val exception =
            assertThrows<ConflictException> {
                interpretationService.addInterpretation(spreadId, duplicateRequest)
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
                interpretationService.addInterpretation(nonExistentSpreadId, request)
            }
        assertEquals("Spread not found", exception.message)
    }

    @Test
    fun `should throw NotFoundException when adding interpretation by non-existent user`() {
        val nonExistentUserId = UUID.randomUUID()
        val request =
            CreateInterpretationRequest(
                text = "This card represents...",
                authorId = nonExistentUserId,
            )

        val exception =
            assertThrows<NotFoundException> {
                interpretationService.addInterpretation(spreadId, request)
            }
        assertEquals("User not found", exception.message)
    }

    @Test
    fun `should allow different users to add interpretations to same spread`() {
        val user2 = userService.createUser(CreateUserRequest(username = "user2"))

        val request1 =
            CreateInterpretationRequest(
                text = "User 1 interpretation",
                authorId = userId,
            )
        val result1 = interpretationService.addInterpretation(spreadId, request1)

        val request2 =
            CreateInterpretationRequest(
                text = "User 2 interpretation",
                authorId = user2.id,
            )
        val result2 = interpretationService.addInterpretation(spreadId, request2)

        assertNotNull(result1.id)
        assertNotNull(result2.id)

        val allInterpretations = interpretationRepository.findAll()
        assertEquals(2, allInterpretations.size)
    }

    @Test
    fun `should update interpretation successfully`() {
        val createRequest =
            CreateInterpretationRequest(
                text = "Original text",
                authorId = userId,
            )
        val created = interpretationService.addInterpretation(spreadId, createRequest)

        val updateRequest = UpdateInterpretationRequest(text = "Updated text", authorId = userId)
        val result = interpretationService.updateInterpretation(spreadId, created.id, userId, updateRequest)

        assertNotNull(result)
        assertEquals(created.id, result.id)
        assertEquals("Updated text", result.text)

        val updated = interpretationRepository.findById(created.id)
        assertTrue(updated.isPresent)
        assertEquals("Updated text", updated.get().text)
    }

    @Test
    fun `should throw NotFoundException when updating non-existent interpretation`() {
        val nonExistentId = UUID.randomUUID()
        val updateRequest = UpdateInterpretationRequest(text = "Updated text", authorId = userId)

        val exception =
            assertThrows<NotFoundException> {
                interpretationService.updateInterpretation(spreadId, nonExistentId, userId, updateRequest)
            }
        assertEquals("Interpretation not found", exception.message)
    }

    @Test
    fun `should throw ForbiddenException when updating another user's interpretation`() {
        val createRequest =
            CreateInterpretationRequest(
                text = "Original text",
                authorId = userId,
            )
        val created = interpretationService.addInterpretation(spreadId, createRequest)

        val otherUser = userService.createUser(CreateUserRequest(username = "otheruser"))
        val updateRequest = UpdateInterpretationRequest(text = "Hacked text", authorId = otherUser.id)

        val exception =
            assertThrows<ForbiddenException> {
                interpretationService.updateInterpretation(spreadId, created.id, otherUser.id, updateRequest)
            }
        assertEquals("You can only edit your own interpretations", exception.message)
    }

    @Test
    fun `should delete interpretation successfully`() {
        val createRequest =
            CreateInterpretationRequest(
                text = "Text to delete",
                authorId = userId,
            )
        val created = interpretationService.addInterpretation(spreadId, createRequest)

        interpretationService.deleteInterpretation(spreadId, created.id, userId)

        val deleted = interpretationRepository.findById(created.id)
        assertTrue(deleted.isEmpty)
    }

    @Test
    fun `should throw NotFoundException when deleting non-existent interpretation`() {
        val nonExistentId = UUID.randomUUID()

        val exception =
            assertThrows<NotFoundException> {
                interpretationService.deleteInterpretation(spreadId, nonExistentId, userId)
            }
        assertEquals("Interpretation not found", exception.message)
    }

    @Test
    fun `should throw ForbiddenException when deleting another user's interpretation`() {
        val createRequest =
            CreateInterpretationRequest(
                text = "Original text",
                authorId = userId,
            )
        val created = interpretationService.addInterpretation(spreadId, createRequest)

        val otherUser = userService.createUser(CreateUserRequest(username = "otheruser"))

        val exception =
            assertThrows<ForbiddenException> {
                interpretationService.deleteInterpretation(spreadId, created.id, otherUser.id)
            }
        assertEquals("You can only delete your own interpretations", exception.message)
    }

    @Test
    fun `should maintain interpretation count after multiple operations`() {
        val user2 = userService.createUser(CreateUserRequest(username = "user2"))

        // Add 2 interpretations
        interpretationService.addInterpretation(
            spreadId,
            CreateInterpretationRequest("Interp 1", userId),
        )
        val interp2 =
            interpretationService.addInterpretation(
                spreadId,
                CreateInterpretationRequest("Interp 2", user2.id),
            )

        assertEquals(2, interpretationRepository.count())

        // Delete one
        interpretationService.deleteInterpretation(spreadId, interp2.id, user2.id)

        assertEquals(1, interpretationRepository.count())
    }

    @Test
    fun `should update interpretation text with long content`() {
        val createRequest =
            CreateInterpretationRequest(
                text = "Short text",
                authorId = userId,
            )
        val created = interpretationService.addInterpretation(spreadId, createRequest)

        val longText = "A".repeat(1000)
        val updateRequest = UpdateInterpretationRequest(text = longText, authorId = userId)
        val result = interpretationService.updateInterpretation(spreadId, created.id, userId, updateRequest)

        assertEquals(longText, result.text)

        val updated = interpretationRepository.findById(created.id)
        assertTrue(updated.isPresent)
        assertEquals(1000, updated.get().text.length)
    }

    @Test
    fun `should preserve createdAt timestamp when updating interpretation`() {
        val createRequest =
            CreateInterpretationRequest(
                text = "Original text",
                authorId = userId,
            )
        val created = interpretationService.addInterpretation(spreadId, createRequest)

        val originalInterpretation = interpretationRepository.findById(created.id).get()
        val originalCreatedAt = originalInterpretation.createdAt

        // Update the interpretation
        val updateRequest = UpdateInterpretationRequest(text = "Updated text", authorId = userId)
        interpretationService.updateInterpretation(spreadId, created.id, userId, updateRequest)

        // Verify createdAt timestamp is preserved (not updated)
        val updated = interpretationRepository.findById(created.id).get()
        assertEquals(originalCreatedAt, updated.createdAt, "createdAt should not change on update")
        assertEquals("Updated text", updated.text, "Text should be updated")
    }

    @Test
    fun `should support multiple interpretations on different spreads by same user`() {
        val spread2 =
            spreadService.createSpread(
                CreateSpreadRequest(
                    question = "Another question?",
                    layoutTypeId = layoutTypeId,
                    authorId = userId,
                ),
            )

        val request1 =
            CreateInterpretationRequest(
                text = "Interpretation for spread 1",
                authorId = userId,
            )
        val result1 = interpretationService.addInterpretation(spreadId, request1)

        val request2 =
            CreateInterpretationRequest(
                text = "Interpretation for spread 2",
                authorId = userId,
            )
        val result2 = interpretationService.addInterpretation(spread2.id, request2)

        assertNotNull(result1.id)
        assertNotNull(result2.id)

        val allInterpretations = interpretationRepository.findAll()
        assertEquals(2, allInterpretations.size)
    }

    @Test
    fun `should get interpretation by id successfully`() {
        val createRequest =
            CreateInterpretationRequest(
                text = "Test interpretation",
                authorId = userId,
            )
        val created = interpretationService.addInterpretation(spreadId, createRequest)

        val result = interpretationService.getInterpretation(spreadId, created.id)

        assertNotNull(result)
        assertEquals(created.id, result.id)
        assertEquals("Test interpretation", result.text)
        assertEquals(userId, result.author.id)
        assertEquals(spreadId, result.spreadId)
    }

    @Test
    fun `should throw NotFoundException when getting non-existent interpretation`() {
        val nonExistentId = UUID.randomUUID()

        val exception =
            assertThrows<NotFoundException> {
                interpretationService.getInterpretation(spreadId, nonExistentId)
            }
        assertEquals("Interpretation not found", exception.message)
    }

    @Test
    fun `should throw NotFoundException when getting interpretation from wrong spread`() {
        val createRequest =
            CreateInterpretationRequest(
                text = "Test interpretation",
                authorId = userId,
            )
        val created = interpretationService.addInterpretation(spreadId, createRequest)

        val differentSpreadId =
            spreadService
                .createSpread(
                    CreateSpreadRequest(
                        question = "Different question?",
                        layoutTypeId = layoutTypeId,
                        authorId = userId,
                    ),
                ).id

        val exception =
            assertThrows<NotFoundException> {
                interpretationService.getInterpretation(differentSpreadId, created.id)
            }
        assertEquals("Interpretation not found in this spread", exception.message)
    }

    @Test
    fun `should get all interpretations for a spread`() {
        val user2 = userService.createUser(CreateUserRequest(username = "user2"))

        val request1 =
            CreateInterpretationRequest(
                text = "First interpretation",
                authorId = userId,
            )
        interpretationService.addInterpretation(spreadId, request1)

        val request2 =
            CreateInterpretationRequest(
                text = "Second interpretation",
                authorId = user2.id,
            )
        interpretationService.addInterpretation(spreadId, request2)

        val result = interpretationService.getInterpretations(spreadId)

        assertNotNull(result)
        assertEquals(2, result.size)
        assertTrue(result.any { it.text == "First interpretation" })
        assertTrue(result.any { it.text == "Second interpretation" })
    }

    @Test
    fun `should return empty list when spread has no interpretations`() {
        val result = interpretationService.getInterpretations(spreadId)

        assertNotNull(result)
        assertEquals(0, result.size)
    }

    @Test
    fun `should return interpretations ordered by creation date descending`() {
        // Create multiple users and interpretations
        val user2 = userService.createUser(CreateUserRequest(username = "user2"))
        val user3 = userService.createUser(CreateUserRequest(username = "user3"))

        val interp1 =
            interpretationService.addInterpretation(
                spreadId,
                CreateInterpretationRequest(text = "First interpretation", authorId = userId),
            )
        val interp2 =
            interpretationService.addInterpretation(
                spreadId,
                CreateInterpretationRequest(text = "Second interpretation", authorId = user2.id),
            )
        val interp3 =
            interpretationService.addInterpretation(
                spreadId,
                CreateInterpretationRequest(text = "Third interpretation", authorId = user3.id),
            )

        val result = interpretationService.getInterpretations(spreadId)

        assertEquals(3, result.size)

        // Verify ordering: each interpretation's createdAt should be >= the next interpretation's createdAt
        for (i in 0 until result.size - 1) {
            assertTrue(
                result[i].createdAt >= result[i + 1].createdAt,
                "Interpretations should be ordered by createdAt descending",
            )
        }

        // Verify all created interpretations are present
        val resultIds = result.map { it.id }.toSet()
        val createdIds = setOf(interp1.id, interp2.id, interp3.id)
        assertEquals(createdIds, resultIds, "All created interpretations should be retrieved")
    }

    @Test
    fun `should throw NotFoundException when getting interpretations for non-existent spread`() {
        val nonExistentSpreadId = UUID.randomUUID()

        val exception =
            assertThrows<NotFoundException> {
                interpretationService.getInterpretations(nonExistentSpreadId)
            }
        assertEquals("Spread not found", exception.message)
    }
}
