package com.github.butvinmitmo.e2e

import com.github.butvinmitmo.shared.dto.CreateInterpretationRequest
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.CreateUserRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests for user deletion cascade behavior.
 *
 * Verifies that when a user is deleted, their spreads and interpretations
 * are eventually deleted via Kafka event consumption in divination-service.
 * Uses polling since cleanup is now asynchronous (eventual consistency).
 */
class UserCascadeDeleteE2ETest : BaseE2ETest() {
    @Test
    fun `deleting user should cascade delete their spreads and interpretations`() {
        loginAsAdmin()

        // Create a test user with MEDIUM role (can create interpretations)
        val testUsername = "cascade_test_${System.currentTimeMillis()}"
        val testPassword = "Test@123"
        val userResponse =
            userClient.createUser(
                currentUserId,
                currentRole,
                CreateUserRequest(username = testUsername, password = testPassword, role = "MEDIUM"),
            )
        val testUserId = userResponse.body!!.id

        // Login as test user and create a spread
        loginAndSetToken(testUsername, testPassword)
        val layoutTypes = tarotClient.getLayoutTypes(currentUserId, currentRole).body!!
        val layoutId = layoutTypes.find { it.name == "ONE_CARD" }!!.id

        val spreadResponse =
            divinationClient.createSpread(
                CreateSpreadRequest("Test spread for cascade deletion", layoutId),
            )
        assertEquals(201, spreadResponse.statusCode.value())
        val spreadId = spreadResponse.body!!.id

        // Add interpretation (user is MEDIUM)
        val interpResponse =
            divinationClient.createInterpretation(
                spreadId,
                CreateInterpretationRequest("Test interpretation for cascade"),
            )
        assertEquals(201, interpResponse.statusCode.value())

        // Verify spread and interpretation exist before deletion
        val spreadBefore = divinationClient.getSpreadById(spreadId)
        assertEquals(200, spreadBefore.statusCode.value())
        assertEquals(1, spreadBefore.body!!.interpretations.size)

        // Delete user as admin
        loginAsAdmin()
        val deleteResponse = userClient.deleteUser(currentUserId, currentRole, testUserId)
        assertEquals(204, deleteResponse.statusCode.value())

        // Verify spread is eventually deleted (async via Kafka)
        awaitStatus(404) {
            divinationClient.getSpreadById(spreadId)
        }

        // Verify user is deleted (should return 404)
        assertThrowsWithStatus(404) {
            userClient.getUserById(currentUserId, currentRole, testUserId)
        }
    }

    @Test
    fun `deleting user should not affect other users spreads`() {
        loginAsAdmin()

        // Create two test users
        val userAUsername = "cascade_userA_${System.currentTimeMillis()}"
        val userBUsername = "cascade_userB_${System.currentTimeMillis()}"
        val password = "Test@123"

        val userAResponse =
            userClient.createUser(
                currentUserId,
                currentRole,
                CreateUserRequest(username = userAUsername, password = password),
            )
        val userAId = userAResponse.body!!.id

        val userBResponse =
            userClient.createUser(
                currentUserId,
                currentRole,
                CreateUserRequest(username = userBUsername, password = password),
            )
        val userBId = userBResponse.body!!.id

        // Login as userA and create a spread
        loginAndSetToken(userAUsername, password)
        val layoutTypes = tarotClient.getLayoutTypes(currentUserId, currentRole).body!!
        val layoutId = layoutTypes.find { it.name == "ONE_CARD" }!!.id

        val spreadAResponse =
            divinationClient.createSpread(
                CreateSpreadRequest("UserA's spread", layoutId),
            )
        val spreadAId = spreadAResponse.body!!.id

        // Login as userB and create a spread
        loginAndSetToken(userBUsername, password)
        val spreadBResponse =
            divinationClient.createSpread(
                CreateSpreadRequest("UserB's spread", layoutId),
            )
        val spreadBId = spreadBResponse.body!!.id

        // Delete userA
        loginAsAdmin()
        val deleteResponse = userClient.deleteUser(currentUserId, currentRole, userAId)
        assertEquals(204, deleteResponse.statusCode.value())

        // UserA's spread should be eventually deleted (async via Kafka)
        awaitStatus(404) {
            divinationClient.getSpreadById(spreadAId)
        }

        // UserB's spread should still exist
        val spreadB = divinationClient.getSpreadById(spreadBId)
        assertEquals(200, spreadB.statusCode.value())
        assertEquals("UserB's spread", spreadB.body!!.question)

        // Cleanup: delete userB
        userClient.deleteUser(currentUserId, currentRole, userBId)
    }
}
