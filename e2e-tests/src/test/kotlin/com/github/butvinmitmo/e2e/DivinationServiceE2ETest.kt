package com.github.butvinmitmo.e2e

import com.github.butvinmitmo.shared.dto.CreateInterpretationRequest
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.UpdateInterpretationRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.util.UUID

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DivinationServiceE2ETest : BaseE2ETest() {
    companion object {
        private lateinit var testUserId: UUID
        private lateinit var adminId: UUID
        private lateinit var oneCardLayoutId: UUID
        private lateinit var threeCardsLayoutId: UUID
        private lateinit var spreadId: UUID
        private lateinit var spreadId2: UUID
        private lateinit var interpretationId: UUID
    }

    @BeforeAll
    fun setupTestData() {
        loginAsAdmin()

        // Create test user
        val userResponse =
            userClient.createUser(
                CreateUserRequest(
                    username = "e2e_divination_user_${System.currentTimeMillis()}",
                    password = "Test@123",
                ),
            )
        testUserId = userResponse.body!!.id

        // Get admin user
        val users = userClient.getUsers().body!!
        adminId = users.find { it.username == "admin" }!!.id

        // Get layout types
        val layoutTypes = tarotClient.getLayoutTypes().body!!
        oneCardLayoutId = layoutTypes.find { it.name == "ONE_CARD" }!!.id
        threeCardsLayoutId = layoutTypes.find { it.name == "THREE_CARDS" }!!.id
    }

    @AfterAll
    fun cleanup() {
        loginAsAdmin()
        // Delete test user (cascades to spreads and interpretations)
        runCatching { userClient.deleteUser(testUserId) }
    }

    @Test
    @Order(1)
    fun `POST spread should create spread via inter-service communication`() {
        loginAsAdmin()
        val request =
            CreateSpreadRequest(
                question = "E2E test question - What does the future hold?",
                layoutTypeId = oneCardLayoutId,
            )
        val response = divinationClient.createSpread(request)

        assertEquals(201, response.statusCode.value())
        assertNotNull(response.body?.id, "Response should contain spread ID")
        spreadId = response.body!!.id
    }

    @Test
    @Order(2)
    fun `GET spread by id should return spread with cards and author`() {
        loginAsAdmin()
        val response = divinationClient.getSpreadById(spreadId)

        assertEquals(200, response.statusCode.value())
        val spread = response.body!!
        assertEquals("E2E test question - What does the future hold?", spread.question)
        assertEquals(1, spread.cards.size, "ONE_CARD spread should have 1 card")
        assertNotNull(spread.author.username, "Author should be fetched via Feign")
        assertEquals("ONE_CARD", spread.layoutType.name)
    }

    @Test
    @Order(3)
    fun `POST spread with THREE_CARDS layout should create spread with 3 cards`() {
        loginAsAdmin()
        val request =
            CreateSpreadRequest(
                question = "E2E test - Past, Present, Future?",
                layoutTypeId = threeCardsLayoutId,
            )
        val response = divinationClient.createSpread(request)

        assertEquals(201, response.statusCode.value())
        spreadId2 = response.body!!.id

        val spread = divinationClient.getSpreadById(spreadId2).body!!
        assertEquals(3, spread.cards.size, "THREE_CARDS spread should have 3 cards")
    }

    @Test
    @Order(4)
    fun `GET spreads with pagination should return spreads list`() {
        loginAsAdmin()
        val response = divinationClient.getSpreads(page = 0, size = 10)

        assertEquals(200, response.statusCode.value())
        assertTrue(response.body!!.size >= 2, "Should have at least 2 spreads")
    }

    @Test
    @Order(5)
    fun `GET spreads with scroll pagination should return with X-After header when more items available`() {
        loginAsAdmin()
        val response = divinationClient.getSpreadsScroll(after = null, size = 1)

        assertEquals(200, response.statusCode.value())
        assertEquals(1, response.body?.size, "Should return exactly 1 spread")

        // X-After header should be present if more items exist
        val xAfterHeader = response.headers["X-After"]?.firstOrNull()
        assertNotNull(xAfterHeader, "X-After header should be present when more items are available")
    }

    @Test
    @Order(6)
    fun `POST interpretation should create interpretation`() {
        loginAsAdmin()
        val request =
            CreateInterpretationRequest(
                text = "E2E test interpretation - The cards suggest great fortune ahead!",
            )
        val response = divinationClient.createInterpretation(spreadId, request)

        assertEquals(201, response.statusCode.value())
        assertNotNull(response.body?.id, "Response should contain interpretation ID")
        interpretationId = response.body!!.id
    }

    @Test
    @Order(7)
    fun `GET spread should now include interpretation`() {
        loginAsAdmin()
        val spread = divinationClient.getSpreadById(spreadId).body!!
        assertEquals(1, spread.interpretations.size, "Spread should have 1 interpretation")
    }

    @Test
    @Order(8)
    fun `POST duplicate interpretation by same author should return 409`() {
        loginAsAdmin()
        val request =
            CreateInterpretationRequest(
                text = "Another interpretation attempt by same author",
            )
        assertThrowsWithStatus(409) {
            divinationClient.createInterpretation(spreadId, request)
        }
    }

    @Test
    @Order(9)
    fun `PUT interpretation should update text`() {
        loginAsAdmin()
        val request =
            UpdateInterpretationRequest(
                text = "Updated E2E interpretation - Even better fortune!",
            )
        val response = divinationClient.updateInterpretation(spreadId, interpretationId, request)

        assertEquals(200, response.statusCode.value())
        assertEquals("Updated E2E interpretation - Even better fortune!", response.body?.text)
    }

    @Test
    @Order(10)
    fun `PUT interpretation by non-author should return 403`() {
        // Create a second test user to test non-author update
        loginAsAdmin()
        val otherUserResponse =
            userClient.createUser(
                CreateUserRequest(
                    username = "e2e_other_user_${System.currentTimeMillis()}",
                    password = "Test@456",
                ),
            )
        val otherUserId = otherUserResponse.body!!.id

        // Login as the other user and try to update admin's interpretation
        loginAndSetToken("e2e_other_user_${otherUserId.toString().takeLast(13)}", "Test@456")
        val request =
            UpdateInterpretationRequest(
                text = "Malicious update attempt",
            )
        assertThrowsWithStatus(403) {
            divinationClient.updateInterpretation(spreadId, interpretationId, request)
        }

        // Cleanup: delete the test user
        loginAsAdmin()
        userClient.deleteUser(otherUserId)
    }

    @Test
    @Order(11)
    fun `POST spread with non-existent layout type should return 404`() {
        loginAsAdmin()
        val fakeId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        val request =
            CreateSpreadRequest(
                question = "This should fail",
                layoutTypeId = fakeId,
            )
        assertThrowsWithStatus(404) {
            divinationClient.createSpread(request)
        }
    }
}
