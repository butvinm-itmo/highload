package com.github.butvinmitmo.e2e

import com.github.butvinmitmo.shared.dto.CreateInterpretationRequest
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.CreateUserRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.util.UUID

/**
 * Tests for cascade delete behavior after removing cross-service FK constraints.
 *
 * When a user is deleted:
 * 1. user-service calls divination-service internal endpoint to delete user's data
 * 2. All spreads authored by the user are deleted
 * 3. All interpretations authored by the user are deleted
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CascadeDeleteE2ETest : BaseE2ETest() {
    companion object {
        private lateinit var testUserId: UUID
        private lateinit var spreadId1: UUID
        private lateinit var spreadId2: UUID
        private lateinit var interpretationId: UUID
        private lateinit var oneCardLayoutId: UUID
    }

    @BeforeAll
    fun setupTestData() {
        // Create test user
        val userResponse =
            userClient.createUser(
                CreateUserRequest(username = "e2e_cascade_user_${System.currentTimeMillis()}"),
            )
        testUserId = userResponse.body!!.id

        // Get layout type
        val layoutTypes = tarotClient.getLayoutTypes().body!!
        oneCardLayoutId = layoutTypes.find { it.name == "ONE_CARD" }!!.id

        // Create spreads
        spreadId1 =
            divinationClient
                .createSpread(
                    CreateSpreadRequest("Cascade test spread 1", oneCardLayoutId, testUserId),
                ).body!!
                .id

        spreadId2 =
            divinationClient
                .createSpread(
                    CreateSpreadRequest("Cascade test spread 2", oneCardLayoutId, testUserId),
                ).body!!
                .id

        // Create interpretation
        interpretationId =
            divinationClient
                .createInterpretation(
                    spreadId1,
                    CreateInterpretationRequest("Cascade test interpretation", testUserId),
                ).body!!
                .id
    }

    @Test
    @Order(1)
    fun `spreads and interpretations exist before user deletion`() {
        // Verify spreads exist
        val spread1 = divinationClient.getSpreadById(spreadId1).body!!
        assertEquals(spreadId1, spread1.id)
        assertEquals(1, spread1.interpretations.size)

        val spread2 = divinationClient.getSpreadById(spreadId2).body!!
        assertEquals(spreadId2, spread2.id)

        // Verify user exists
        val user = userClient.getUserById(testUserId).body!!
        assertEquals(testUserId, user.id)
    }

    @Test
    @Order(2)
    fun `DELETE user should cascade delete all user spreads and interpretations`() {
        // Delete user - this should trigger cascade delete in divination-service
        val response = userClient.deleteUser(testUserId)
        assertEquals(204, response.statusCode.value())

        // Verify user is deleted
        assertThrowsWithStatus(404) {
            userClient.getUserById(testUserId)
        }

        // Verify spread 1 is deleted (had the interpretation)
        assertThrowsWithStatus(404) {
            divinationClient.getSpreadById(spreadId1)
        }

        // Verify spread 2 is deleted
        assertThrowsWithStatus(404) {
            divinationClient.getSpreadById(spreadId2)
        }
    }
}
