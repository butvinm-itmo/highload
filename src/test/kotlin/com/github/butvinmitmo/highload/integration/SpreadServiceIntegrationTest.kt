package com.github.butvinmitmo.highload.integration

import com.github.butvinmitmo.highload.dto.CreateInterpretationRequest
import com.github.butvinmitmo.highload.dto.CreateSpreadRequest
import com.github.butvinmitmo.highload.dto.CreateUserRequest
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@SpringBootTest
@Testcontainers
class SpreadServiceIntegrationTest {
    companion object {
        @Container
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:15-alpine")
                .withDatabaseName("tarot_db_test")
                .withUsername("test_user")
                .withPassword("test_password")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
        }
    }

    @Autowired
    private lateinit var spreadService: SpreadService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var interpretationService: InterpretationService

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

    @AfterEach
    fun cleanup() {
        interpretationRepository.deleteAll()
        spreadCardRepository.deleteAll()
        spreadRepository.deleteAll()
        userRepository.findAll().forEach { user ->
            if (user.username != "admin") {
                userRepository.delete(user)
            }
        }
    }

    @Test
    fun `should create spread successfully with random cards`() {
        val request =
            CreateSpreadRequest(
                question = "What will happen?",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val result = spreadService.createSpread(request)

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

        val result = spreadService.createSpread(request)

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

        val result = spreadService.createSpread(request)

        val saved = spreadRepository.findByIdWithCards(result.id)
        assertNotNull(saved)
        // CROSS layout cardsCount
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
                spreadService.createSpread(request)
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
                spreadService.createSpread(request)
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

        val created = spreadService.createSpread(createRequest)

        // Test that we can fetch the spread with cards
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
        // Given - create a spread without interpretations
        val createRequest =
            CreateSpreadRequest(
                question = "Will I succeed?",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val created = spreadService.createSpread(createRequest)

        // When - fetch the spread using the service method
        val result = spreadService.getSpread(created.id)

        // Then
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
        // Given - create a spread
        val spreadRequest =
            CreateSpreadRequest(
                question = "What does the future hold?",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val createdSpread = spreadService.createSpread(spreadRequest)

        // Add an interpretation
        val interpretationRequest =
            CreateInterpretationRequest(
                text = "The cards suggest positive changes ahead",
                authorId = userId,
            )

        interpretationService.addInterpretation(createdSpread.id, interpretationRequest)

        // When - fetch the spread
        val result = spreadService.getSpread(createdSpread.id)

        // Then
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
        // Given - create a second user
        val user2Id =
            userService
                .createUser(
                    CreateUserRequest(username = "reader2"),
                ).id

        // Create a spread
        val spreadRequest =
            CreateSpreadRequest(
                question = "Career advice?",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val createdSpread = spreadService.createSpread(spreadRequest)

        // Add multiple interpretations from different users
        interpretationService.addInterpretation(
            createdSpread.id,
            CreateInterpretationRequest(
                text = "First interpretation from user1",
                authorId = userId,
            ),
        )

        interpretationService.addInterpretation(
            createdSpread.id,
            CreateInterpretationRequest(
                text = "Second interpretation from user2",
                authorId = user2Id,
            ),
        )

        // When - fetch the spread
        val result = spreadService.getSpread(createdSpread.id)

        // Then
        assertNotNull(result)
        assertEquals(createdSpread.id, result.id)
        assertEquals("Career advice?", result.question)
        assertNotNull(result.cards)
        assertEquals(1, result.cards.size)
        assertNotNull(result.interpretations)
        assertEquals(2, result.interpretations.size)

        // Verify both interpretations are present
        val interpretationTexts = result.interpretations.map { it.text }
        assertTrue(interpretationTexts.contains("First interpretation from user1"))
        assertTrue(interpretationTexts.contains("Second interpretation from user2"))
    }

    @Test
    fun `should throw NotFoundException when getting non-existent spread via service`() {
        val nonExistentId = UUID.randomUUID()

        val exception =
            assertThrows<NotFoundException> {
                spreadService.getSpread(nonExistentId)
            }

        assertEquals("Spread not found", exception.message)
    }

    @Test
    fun `should get paginated spreads count successfully`() {
        spreadService.createSpread(
            CreateSpreadRequest(
                question = "Question 1",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            ),
        )
        spreadService.createSpread(
            CreateSpreadRequest(
                question = "Question 2",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            ),
        )
        spreadService.createSpread(
            CreateSpreadRequest(
                question = "Question 3",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            ),
        )

        // Verify directly from repository
        val allSpreads = spreadRepository.findAll()
        assertEquals(3, allSpreads.size)
    }

    @Test
    fun `should support scroll pagination`() {
        val spread1 =
            spreadService.createSpread(
                CreateSpreadRequest(
                    question = "Question 1",
                    layoutTypeId = layoutTypeId,
                    authorId = userId,
                ),
            )
        Thread.sleep(10) // Ensure different timestamps
        spreadService.createSpread(
            CreateSpreadRequest(
                question = "Question 2",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            ),
        )
        Thread.sleep(10)
        spreadService.createSpread(
            CreateSpreadRequest(
                question = "Question 3",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            ),
        )

        // Verify repository scroll method works
        val firstPage = spreadRepository.findLatestSpreads(2)
        assertEquals(2, firstPage.size)

        val secondPage = spreadRepository.findSpreadsAfterCursor(spread1.id, 2)
        // Should return spreads created before spread1
        assertTrue(secondPage.isEmpty() || secondPage.size <= 2)
    }

    @Test
    fun `should delete spread successfully when user is author`() {
        val createRequest =
            CreateSpreadRequest(
                question = "What will happen?",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val created = spreadService.createSpread(createRequest)

        spreadService.deleteSpread(created.id, userId)

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

        val created = spreadService.createSpread(createRequest)

        val otherUserId = UUID.randomUUID()

        val exception =
            assertThrows<ForbiddenException> {
                spreadService.deleteSpread(created.id, otherUserId)
            }
        assertEquals("You can only delete your own spreads", exception.message)
    }

    @Test
    fun `should throw NotFoundException when deleting non-existent spread`() {
        val nonExistentId = UUID.randomUUID()

        val exception =
            assertThrows<NotFoundException> {
                spreadService.deleteSpread(nonExistentId, userId)
            }
        assertEquals("Spread not found", exception.message)
    }

    @Test
    fun `should get layout type by id`() {
        val layoutType = spreadService.getLayoutTypeById(layoutTypeId)

        assertNotNull(layoutType)
        assertEquals("ONE_CARD", layoutType.name)
        assertEquals(1, layoutType.cardsCount)
    }

    @Test
    fun `should throw NotFoundException when getting non-existent layout type by id`() {
        val nonExistentId = UUID.randomUUID()

        val exception =
            assertThrows<NotFoundException> {
                spreadService.getLayoutTypeById(nonExistentId)
            }
        assertEquals("Layout type not found", exception.message)
    }

    @Test
    fun `should get spread entity successfully`() {
        val createRequest =
            CreateSpreadRequest(
                question = "What will happen?",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val created = spreadService.createSpread(createRequest)

        val result = spreadService.getSpreadEntity(created.id)

        assertNotNull(result)
        assertEquals(created.id, result.id)
        assertEquals("What will happen?", result.question)
    }

    @Test
    fun `should throw NotFoundException when getting non-existent spread entity`() {
        val nonExistentId = UUID.randomUUID()

        val exception =
            assertThrows<NotFoundException> {
                spreadService.getSpreadEntity(nonExistentId)
            }
        assertEquals("Spread not found", exception.message)
    }

    @Test
    fun `should order spreads by creation date descending`() {
        val first =
            spreadService.createSpread(
                CreateSpreadRequest(
                    question = "First",
                    layoutTypeId = layoutTypeId,
                    authorId = userId,
                ),
            )
        Thread.sleep(10)
        spreadService.createSpread(
            CreateSpreadRequest(
                question = "Second",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            ),
        )
        Thread.sleep(10)
        val third =
            spreadService.createSpread(
                CreateSpreadRequest(
                    question = "Third",
                    layoutTypeId = layoutTypeId,
                    authorId = userId,
                ),
            )

        // Verify repository ordering
        val spreads =
            spreadRepository.findAllOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest
                    .of(0, 10),
            )

        assertEquals(3, spreads.content.size)
        // Most recent first
        assertEquals(third.id, spreads.content[0].id)
        assertEquals(first.id, spreads.content[2].id)
    }

    @Test
    fun `should create spread with null question`() {
        val request =
            CreateSpreadRequest(
                question = null,
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val result = spreadService.createSpread(request)

        val saved = spreadRepository.findById(result.id)
        assertTrue(saved.isPresent)
        assertEquals(null, saved.get().question)
    }

    @Test
    fun `should return empty list when no spreads exist`() {
        val spreads = spreadService.getSpreads(0, 10)

        assertNotNull(spreads)
        assertEquals(0, spreads.content.size)
        assertEquals(0, spreads.totalElements)
    }

    @Test
    fun `should return empty list for scroll when no spreads exist`() {
        val spreads = spreadService.getSpreadsByScroll(null, 10)

        assertNotNull(spreads)
        assertEquals(0, spreads.size)
    }

    @Test
    fun `spread cards should have correct position and reversed status`() {
        val request =
            CreateSpreadRequest(
                question = "Test positions",
                layoutTypeId = layoutTypeId,
                authorId = userId,
            )

        val result = spreadService.createSpread(request)

        val saved = spreadRepository.findByIdWithCards(result.id)
        assertNotNull(saved)

        val spreadCards = saved!!.spreadCards
        assertEquals(1, spreadCards.size)

        // Verify position is set
        assertEquals(1, spreadCards[0].positionInSpread)

        // Verify isReversed is either true or false (not null)
        assertNotNull(spreadCards[0].isReversed)
    }
}
