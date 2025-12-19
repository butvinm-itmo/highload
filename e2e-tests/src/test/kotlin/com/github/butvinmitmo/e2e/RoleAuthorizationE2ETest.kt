package com.github.butvinmitmo.e2e

import com.github.butvinmitmo.shared.dto.CreateInterpretationRequest
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.UpdateInterpretationRequest
import com.github.butvinmitmo.shared.dto.UpdateUserRequest
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

/**
 * Comprehensive E2E tests for the 3-role authorization model.
 *
 * Tests verify the following role permissions:
 * - USER: create spreads, read spreads/interpretations, get users (CANNOT add interpretations)
 * - MEDIUM: all USER permissions + add interpretations to any spread
 * - ADMIN: complete system access (user CRUD, create spreads/interpretations, bypass author-only checks)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RoleAuthorizationE2ETest : BaseE2ETest() {
    companion object {
        private lateinit var userId: UUID
        private lateinit var userUsername: String
        private lateinit var mediumId: UUID
        private lateinit var mediumUsername: String
        private lateinit var oneCardLayoutId: UUID
        private lateinit var userSpreadId: UUID
        private lateinit var mediumSpreadId: UUID
        private lateinit var mediumInterpretationId: UUID

        private const val USER_PASSWORD = "User@123"
        private const val MEDIUM_PASSWORD = "Medium@456"
    }

    @BeforeAll
    fun setupTestData() {
        loginAsAdmin()

        // Create USER role user
        userUsername = "e2e_role_user_${System.currentTimeMillis()}"
        val userResponse =
            userClient.createUser(
                CreateUserRequest(
                    username = userUsername,
                    password = USER_PASSWORD,
                ),
            )
        userId = userResponse.body!!.id

        // Create MEDIUM role user
        mediumUsername = "e2e_role_medium_${System.currentTimeMillis()}"
        val mediumResponse =
            userClient.createUser(
                CreateUserRequest(
                    username = mediumUsername,
                    password = MEDIUM_PASSWORD,
                    role = "MEDIUM",
                ),
            )
        mediumId = mediumResponse.body!!.id

        // Get layout type
        val layoutTypes = tarotClient.getLayoutTypes().body!!
        oneCardLayoutId = layoutTypes.find { it.name == "ONE_CARD" }!!.id
    }

    @AfterAll
    fun cleanup() {
        loginAsAdmin()
        runCatching { userClient.deleteUser(userId) }
        runCatching { userClient.deleteUser(mediumId) }
    }

    // ============================================
    // USER Role Tests
    // ============================================

    @Test
    @Order(1)
    fun `USER can create spreads`() {
        loginAndSetToken(userUsername, USER_PASSWORD)
        val response =
            divinationClient.createSpread(
                CreateSpreadRequest("USER's spread", oneCardLayoutId),
            )
        assertEquals(201, response.statusCode.value())
        userSpreadId = response.body!!.id
    }

    @Test
    @Order(2)
    fun `USER cannot create interpretations (403)`() {
        loginAndSetToken(userUsername, USER_PASSWORD)
        assertThrowsWithStatus(403) {
            divinationClient.createInterpretation(
                userSpreadId,
                CreateInterpretationRequest("USER cannot do this"),
            )
        }
    }

    @Test
    @Order(3)
    fun `USER can read spreads`() {
        loginAndSetToken(userUsername, USER_PASSWORD)
        val response = divinationClient.getSpreads(page = 0, size = 10)
        assertEquals(200, response.statusCode.value())
        assertTrue(response.body!!.isNotEmpty())
    }

    @Test
    @Order(4)
    fun `USER can get users list`() {
        loginAndSetToken(userUsername, USER_PASSWORD)
        val response = userClient.getUsers()
        assertEquals(200, response.statusCode.value())
        assertTrue(response.body!!.isNotEmpty())
    }

    @Test
    @Order(5)
    fun `USER cannot create users (403)`() {
        loginAndSetToken(userUsername, USER_PASSWORD)
        assertThrowsWithStatus(403) {
            userClient.createUser(
                CreateUserRequest(
                    username = "should_not_exist_${System.currentTimeMillis()}",
                    password = "Test@123",
                ),
            )
        }
    }

    @Test
    @Order(6)
    fun `USER cannot update users (403)`() {
        loginAndSetToken(userUsername, USER_PASSWORD)
        assertThrowsWithStatus(403) {
            userClient.updateUser(
                mediumId,
                UpdateUserRequest(username = "hacked_username"),
            )
        }
    }

    @Test
    @Order(7)
    fun `USER cannot delete users (403)`() {
        loginAndSetToken(userUsername, USER_PASSWORD)
        assertThrowsWithStatus(403) {
            userClient.deleteUser(mediumId)
        }
    }

    // ============================================
    // MEDIUM Role Tests
    // ============================================

    @Test
    @Order(10)
    fun `MEDIUM can create spreads`() {
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)
        val response =
            divinationClient.createSpread(
                CreateSpreadRequest("MEDIUM's spread", oneCardLayoutId),
            )
        assertEquals(201, response.statusCode.value())
        mediumSpreadId = response.body!!.id
    }

    @Test
    @Order(11)
    fun `MEDIUM can create interpretations`() {
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)
        val response =
            divinationClient.createInterpretation(
                mediumSpreadId,
                CreateInterpretationRequest("MEDIUM's interpretation"),
            )
        assertEquals(201, response.statusCode.value())
        mediumInterpretationId = response.body!!.id
    }

    @Test
    @Order(12)
    fun `MEDIUM can interpret USER's spread`() {
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)
        val response =
            divinationClient.createInterpretation(
                userSpreadId,
                CreateInterpretationRequest("MEDIUM interprets USER's spread"),
            )
        assertEquals(201, response.statusCode.value())
    }

    @Test
    @Order(13)
    fun `MEDIUM can read all spreads and interpretations`() {
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)
        val response = divinationClient.getSpreadById(userSpreadId)
        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertTrue(response.body!!.interpretations.isNotEmpty())
    }

    @Test
    @Order(14)
    fun `MEDIUM cannot delete USER's spread (403)`() {
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)
        assertThrowsWithStatus(403) {
            divinationClient.deleteSpread(userSpreadId)
        }
    }

    @Test
    @Order(15)
    fun `MEDIUM cannot create users (403)`() {
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)
        assertThrowsWithStatus(403) {
            userClient.createUser(
                CreateUserRequest(
                    username = "should_not_exist_${System.currentTimeMillis()}",
                    password = "Test@123",
                ),
            )
        }
    }

    @Test
    @Order(16)
    fun `MEDIUM cannot update users (403)`() {
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)
        assertThrowsWithStatus(403) {
            userClient.updateUser(
                userId,
                UpdateUserRequest(username = "hacked_username"),
            )
        }
    }

    @Test
    @Order(17)
    fun `MEDIUM cannot delete users (403)`() {
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)
        assertThrowsWithStatus(403) {
            userClient.deleteUser(userId)
        }
    }

    // ============================================
    // ADMIN Role Tests
    // ============================================

    @Test
    @Order(20)
    fun `ADMIN can create users with any role`() {
        loginAsAdmin()

        // Create USER
        val userResponse =
            userClient.createUser(
                CreateUserRequest(
                    username = "e2e_admin_created_user_${System.currentTimeMillis()}",
                    password = "Test@123",
                    role = "USER",
                ),
            )
        assertEquals(201, userResponse.statusCode.value())
        val createdUser = userClient.getUserById(userResponse.body!!.id).body!!
        assertEquals("USER", createdUser.role)

        // Create MEDIUM
        val mediumResponse =
            userClient.createUser(
                CreateUserRequest(
                    username = "e2e_admin_created_medium_${System.currentTimeMillis()}",
                    password = "Test@123",
                    role = "MEDIUM",
                ),
            )
        assertEquals(201, mediumResponse.statusCode.value())
        val createdMedium = userClient.getUserById(mediumResponse.body!!.id).body!!
        assertEquals("MEDIUM", createdMedium.role)

        // Cleanup
        userClient.deleteUser(userResponse.body!!.id)
        userClient.deleteUser(mediumResponse.body!!.id)
    }

    @Test
    @Order(21)
    fun `ADMIN can update user role`() {
        loginAsAdmin()

        // Create a USER
        val createResponse =
            userClient.createUser(
                CreateUserRequest(
                    username = "e2e_role_change_${System.currentTimeMillis()}",
                    password = "Test@123",
                    role = "USER",
                ),
            )
        val createdUserId = createResponse.body!!.id
        val createdUserRole = userClient.getUserById(createdUserId).body!!.role
        assertEquals("USER", createdUserRole)

        // Update to MEDIUM
        val updateResponse =
            userClient.updateUser(
                createdUserId,
                UpdateUserRequest(role = "MEDIUM"),
            )
        assertEquals(200, updateResponse.statusCode.value())
        assertEquals("MEDIUM", updateResponse.body!!.role)

        // Cleanup
        userClient.deleteUser(createdUserId)
    }

    @Test
    @Order(22)
    fun `ADMIN can delete any spread (bypass author check)`() {
        // First create a spread as USER
        loginAndSetToken(userUsername, USER_PASSWORD)
        val createResponse =
            divinationClient.createSpread(
                CreateSpreadRequest("USER spread to be deleted by ADMIN", oneCardLayoutId),
            )
        val spreadToDelete = createResponse.body!!.id

        // ADMIN deletes USER's spread
        loginAsAdmin()
        val deleteResponse = divinationClient.deleteSpread(spreadToDelete)
        assertEquals(204, deleteResponse.statusCode.value())

        // Verify spread is deleted
        assertThrowsWithStatus(404) {
            divinationClient.getSpreadById(spreadToDelete)
        }
    }

    @Test
    @Order(23)
    fun `ADMIN can update any interpretation (bypass author check)`() {
        // ADMIN updates MEDIUM's interpretation
        loginAsAdmin()
        val updateResponse =
            divinationClient.updateInterpretation(
                mediumSpreadId,
                mediumInterpretationId,
                UpdateInterpretationRequest("ADMIN updated MEDIUM's interpretation"),
            )
        assertEquals(200, updateResponse.statusCode.value())
        assertEquals("ADMIN updated MEDIUM's interpretation", updateResponse.body!!.text)
    }

    @Test
    @Order(24)
    fun `ADMIN can delete any interpretation (bypass author check)`() {
        // Create interpretation as MEDIUM
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)
        val createResponse =
            divinationClient.createSpread(
                CreateSpreadRequest("Spread for ADMIN delete test", oneCardLayoutId),
            )
        val testSpreadId = createResponse.body!!.id

        val interpretationResponse =
            divinationClient.createInterpretation(
                testSpreadId,
                CreateInterpretationRequest("MEDIUM interpretation to be deleted by ADMIN"),
            )
        val interpretationToDelete = interpretationResponse.body!!.id

        // ADMIN deletes MEDIUM's interpretation
        loginAsAdmin()
        val deleteResponse = divinationClient.deleteInterpretation(testSpreadId, interpretationToDelete)
        assertEquals(204, deleteResponse.statusCode.value())

        // Verify interpretation is deleted
        val spread = divinationClient.getSpreadById(testSpreadId).body!!
        assertTrue(
            spread.interpretations.none { it.id == interpretationToDelete },
            "Interpretation should be deleted",
        )

        // Cleanup - delete the spread
        loginAndSetToken(mediumUsername, MEDIUM_PASSWORD)
        divinationClient.deleteSpread(testSpreadId)
    }

    @Test
    @Order(25)
    fun `ADMIN can create spreads`() {
        loginAsAdmin()
        val response =
            divinationClient.createSpread(
                CreateSpreadRequest("ADMIN's spread", oneCardLayoutId),
            )
        assertEquals(201, response.statusCode.value())

        // Cleanup
        divinationClient.deleteSpread(response.body!!.id)
    }

    @Test
    @Order(26)
    fun `ADMIN can create interpretations`() {
        loginAsAdmin()
        val response =
            divinationClient.createInterpretation(
                mediumSpreadId,
                CreateInterpretationRequest("ADMIN's interpretation"),
            )
        assertEquals(201, response.statusCode.value())
    }
}
