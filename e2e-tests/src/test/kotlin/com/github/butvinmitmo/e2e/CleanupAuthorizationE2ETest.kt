package com.github.butvinmitmo.e2e

import com.github.butvinmitmo.shared.dto.CreateInterpretationRequest
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.CreateUserRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.util.UUID

/**
 * Tests for authorization and cleanup operations.
 *
 * This test suite verifies JWT-based authorization by creating multiple users
 * and testing that users can only delete their own resources.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CleanupAuthorizationE2ETest : BaseE2ETest() {
    companion object {
        private lateinit var userAId: UUID
        private lateinit var userAUsername: String
        private lateinit var userBId: UUID
        private lateinit var userBUsername: String
        private lateinit var oneCardLayoutId: UUID
        private lateinit var spreadId: UUID
        private lateinit var interpretationId: UUID

        private const val USER_A_PASSWORD = "UserA@123"
        private const val USER_B_PASSWORD = "UserB@456"
    }

    @BeforeAll
    fun setupTestData() {
        loginAsAdmin()

        // Create userA and userB for authorization testing
        userAUsername = "e2e_auth_userA_${System.currentTimeMillis()}"
        val userAResponse =
            userClient.createUser(
                CreateUserRequest(
                    username = userAUsername,
                    password = USER_A_PASSWORD,
                ),
            )
        userAId = userAResponse.body!!.id

        userBUsername = "e2e_auth_userB_${System.currentTimeMillis()}"
        val userBResponse =
            userClient.createUser(
                CreateUserRequest(
                    username = userBUsername,
                    password = USER_B_PASSWORD,
                ),
            )
        userBId = userBResponse.body!!.id

        // Get layout type
        val layoutTypes = tarotClient.getLayoutTypes().body!!
        oneCardLayoutId = layoutTypes.find { it.name == "ONE_CARD" }!!.id
    }

    @AfterAll
    fun cleanup() {
        loginAsAdmin()
        // Delete both test users (cascades to spreads and interpretations)
        runCatching { userClient.deleteUser(userAId) }
        runCatching { userClient.deleteUser(userBId) }
    }

    @Test
    @Order(1)
    fun `UserA creates spread`() {
        loginAndSetToken(userAUsername, USER_A_PASSWORD)
        val response =
            divinationClient.createSpread(
                CreateSpreadRequest("UserA's test spread", oneCardLayoutId),
            )
        assertEquals(201, response.statusCode.value())
        spreadId = response.body!!.id
    }

    @Test
    @Order(2)
    fun `UserB cannot delete UserA's spread (403)`() {
        loginAndSetToken(userBUsername, USER_B_PASSWORD)
        assertThrowsWithStatus(403) {
            divinationClient.deleteSpread(spreadId)
        }
    }

    @Test
    @Order(3)
    fun `UserA can delete own spread (204)`() {
        loginAndSetToken(userAUsername, USER_A_PASSWORD)
        val response = divinationClient.deleteSpread(spreadId)
        assertEquals(204, response.statusCode.value())

        // Verify spread is deleted
        assertThrowsWithStatus(404) {
            divinationClient.getSpreadById(spreadId)
        }
    }

    @Test
    @Order(4)
    fun `UserA creates another spread for interpretation test`() {
        loginAndSetToken(userAUsername, USER_A_PASSWORD)
        val response =
            divinationClient.createSpread(
                CreateSpreadRequest("UserA's spread for interpretation", oneCardLayoutId),
            )
        assertEquals(201, response.statusCode.value())
        spreadId = response.body!!.id
    }

    @Test
    @Order(5)
    fun `Admin adds interpretation to UserA's spread`() {
        // TODO: In Phase 4, create userB as MEDIUM role and use userB here instead of admin
        loginAsAdmin()
        val response =
            divinationClient.createInterpretation(
                spreadId,
                CreateInterpretationRequest("Admin interpretation on UserA's spread"),
            )
        assertEquals(201, response.statusCode.value())
        interpretationId = response.body!!.id
    }

    @Test
    @Order(6)
    fun `UserA cannot delete admin's interpretation (403)`() {
        loginAndSetToken(userAUsername, USER_A_PASSWORD)
        assertThrowsWithStatus(403) {
            divinationClient.deleteInterpretation(spreadId, interpretationId)
        }
    }

    @Test
    @Order(7)
    fun `Admin can delete own interpretation (204)`() {
        loginAsAdmin()
        val response = divinationClient.deleteInterpretation(spreadId, interpretationId)
        assertEquals(204, response.statusCode.value())

        // Verify interpretation is deleted
        loginAndSetToken(userAUsername, USER_A_PASSWORD)
        val spread = divinationClient.getSpreadById(spreadId).body!!
        assertEquals(0, spread.interpretations.size, "Spread should have no interpretations after deletion")
    }

    @Test
    @Order(8)
    fun `UserA deletes own spread after interpretation is removed (204)`() {
        loginAndSetToken(userAUsername, USER_A_PASSWORD)
        val response = divinationClient.deleteSpread(spreadId)
        assertEquals(204, response.statusCode.value())
    }
}
