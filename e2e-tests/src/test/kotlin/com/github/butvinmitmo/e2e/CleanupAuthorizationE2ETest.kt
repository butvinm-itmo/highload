package com.github.butvinmitmo.e2e

import com.github.butvinmitmo.shared.dto.CreateInterpretationRequest
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.DeleteRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.util.UUID

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CleanupAuthorizationE2ETest : BaseE2ETest() {
    companion object {
        private lateinit var testUserId: UUID
        private lateinit var adminId: UUID
        private lateinit var spreadId: UUID
        private lateinit var spreadId2: UUID
        private lateinit var interpretationId: UUID
        private lateinit var oneCardLayoutId: UUID
    }

    @BeforeAll
    fun setupTestData() {
        loginAsAdmin()

        // Create test user
        val userResponse =
            userClient.createUser(
                CreateUserRequest(
                    username = "e2e_cleanup_user_${System.currentTimeMillis()}",
                    password = "Test@123",
                ),
            )
        testUserId = userResponse.body!!.id

        // Get admin user
        val users = userClient.getUsers().body!!
        adminId = users.find { it.username == "admin" }!!.id

        // Get layout type
        val layoutTypes = tarotClient.getLayoutTypes().body!!
        oneCardLayoutId = layoutTypes.find { it.name == "ONE_CARD" }!!.id

        // Create spreads
        spreadId =
            divinationClient
                .createSpread(
                    CreateSpreadRequest("Cleanup test spread 1", oneCardLayoutId, testUserId),
                ).body!!
                .id

        spreadId2 =
            divinationClient
                .createSpread(
                    CreateSpreadRequest("Cleanup test spread 2", oneCardLayoutId, testUserId),
                ).body!!
                .id

        // Create interpretation
        interpretationId =
            divinationClient
                .createInterpretation(
                    spreadId,
                    CreateInterpretationRequest("Cleanup test interpretation", testUserId),
                ).body!!
                .id
    }

    @Test
    @Order(1)
    fun `DELETE interpretation should succeed for author`() {
        loginAsAdmin()
        val response =
            divinationClient.deleteInterpretation(
                spreadId,
                interpretationId,
                DeleteRequest(testUserId),
            )
        assertEquals(204, response.statusCode.value())

        // Verify interpretation is deleted
        val spread = divinationClient.getSpreadById(spreadId).body!!
        assertEquals(0, spread.interpretations.size, "Spread should have no interpretations after deletion")
    }

    @Test
    @Order(2)
    fun `DELETE spread should succeed for author`() {
        loginAsAdmin()
        val response = divinationClient.deleteSpread(spreadId, DeleteRequest(testUserId))
        assertEquals(204, response.statusCode.value())

        // Verify spread is deleted
        assertThrowsWithStatus(404) {
            divinationClient.getSpreadById(spreadId)
        }
    }

    @Test
    @Order(3)
    fun `DELETE spread by non-author should return 403`() {
        loginAsAdmin()
        assertThrowsWithStatus(403) {
            divinationClient.deleteSpread(spreadId2, DeleteRequest(adminId))
        }
    }

    @Test
    @Order(4)
    fun `DELETE second spread for cleanup should succeed`() {
        loginAsAdmin()
        val response = divinationClient.deleteSpread(spreadId2, DeleteRequest(testUserId))
        assertEquals(204, response.statusCode.value())
    }

    @Test
    @Order(5)
    fun `DELETE user should succeed and verify deletion`() {
        loginAsAdmin()
        val response = userClient.deleteUser(testUserId)
        assertEquals(204, response.statusCode.value())

        // Verify user is deleted
        assertThrowsWithStatus(404) {
            userClient.getUserById(testUserId)
        }
    }
}
