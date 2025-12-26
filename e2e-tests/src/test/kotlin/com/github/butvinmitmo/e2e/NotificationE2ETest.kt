package com.github.butvinmitmo.e2e

import com.github.butvinmitmo.shared.client.NotificationServiceClient
import com.github.butvinmitmo.shared.dto.CreateInterpretationRequest
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.NotificationType
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * E2E tests for notification functionality.
 *
 * Tests the full flow: spread creation -> interpretation -> notification -> read notification
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class NotificationE2ETest : BaseE2ETest() {
    @Autowired
    protected lateinit var notificationClient: NotificationServiceClient

    companion object {
        private lateinit var spreadAuthorId: UUID
        private lateinit var spreadAuthorUsername: String
        private lateinit var mediumUserId: UUID
        private lateinit var mediumUsername: String
        private lateinit var spreadId: UUID
        private lateinit var notificationId: UUID
        private var oneCardLayoutId: UUID? = null
    }

    @BeforeAll
    fun setupTestData() {
        loginAsAdmin()

        // Create a USER who will create spreads
        spreadAuthorUsername = "e2e_spread_author_${System.currentTimeMillis()}"
        val spreadAuthorResponse =
            userClient.createUser(
                currentUserId,
                currentRole,
                CreateUserRequest(
                    username = spreadAuthorUsername,
                    password = "Test@123",
                ),
            )
        spreadAuthorId = spreadAuthorResponse.body!!.id

        // Create a MEDIUM who will add interpretations
        mediumUsername = "e2e_medium_${System.currentTimeMillis()}"
        val mediumResponse =
            userClient.createUser(
                currentUserId,
                currentRole,
                CreateUserRequest(
                    username = mediumUsername,
                    password = "Test@456",
                    role = "MEDIUM",
                ),
            )
        mediumUserId = mediumResponse.body!!.id

        // Get ONE_CARD layout type
        val layoutTypes = tarotClient.getLayoutTypes(currentUserId, currentRole).body!!
        oneCardLayoutId = layoutTypes.find { it.name == "ONE_CARD" }?.id
    }

    @AfterAll
    fun cleanup() {
        loginAsAdmin()
        runCatching { userClient.deleteUser(currentUserId, currentRole, spreadAuthorId) }
        runCatching { userClient.deleteUser(currentUserId, currentRole, mediumUserId) }
    }

    @Test
    @Order(1)
    fun `spread author should have zero unread notifications initially`() {
        loginAndSetToken(spreadAuthorUsername, "Test@123")

        val response = notificationClient.getUnreadCount()
        assertEquals(200, response.statusCode.value())
        assertEquals(0L, response.body!!.count)
    }

    @Test
    @Order(2)
    fun `spread author creates a spread`() {
        loginAndSetToken(spreadAuthorUsername, "Test@123")

        val request =
            CreateSpreadRequest(
                question = "E2E notification test - What does the future hold?",
                layoutTypeId = oneCardLayoutId!!,
            )
        val response = divinationClient.createSpread(request)

        assertEquals(201, response.statusCode.value())
        spreadId = response.body!!.id
    }

    @Test
    @Order(3)
    fun `medium adds interpretation to spread author's spread`() {
        loginAndSetToken(mediumUsername, "Test@456")

        val request =
            CreateInterpretationRequest(
                text = "E2E test interpretation - The cards suggest great opportunities ahead!",
            )
        val response = divinationClient.createInterpretation(spreadId, request)

        assertEquals(201, response.statusCode.value())
    }

    @Test
    @Order(4)
    fun `spread author should receive notification after interpretation is added`() {
        loginAndSetToken(spreadAuthorUsername, "Test@123")

        // Wait for Kafka event to be processed (with Awaitility)
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted {
                val response = notificationClient.getUnreadCount()
                assertEquals(200, response.statusCode.value())
                assertEquals(1L, response.body!!.count, "Should have 1 unread notification")
            }
    }

    @Test
    @Order(5)
    fun `spread author can see the notification with correct details`() {
        loginAndSetToken(spreadAuthorUsername, "Test@123")

        val response = notificationClient.getNotifications()
        assertEquals(200, response.statusCode.value())

        val notifications = response.body!!
        assertEquals(1, notifications.size)

        val notification = notifications[0]
        notificationId = notification.id

        assertEquals(NotificationType.NEW_INTERPRETATION, notification.type)
        assertEquals(spreadId, notification.spreadId)
        assertNotNull(notification.interpretationId)
        assertTrue(notification.message.contains(mediumUsername), "Message should contain medium's username")
        assertFalse(notification.isRead, "Notification should be unread")
    }

    @Test
    @Order(6)
    fun `spread author can mark notification as read`() {
        loginAndSetToken(spreadAuthorUsername, "Test@123")

        val response = notificationClient.markAsRead(notificationId)
        assertEquals(200, response.statusCode.value())

        val notification = response.body!!
        assertTrue(notification.isRead, "Notification should be marked as read")
    }

    @Test
    @Order(7)
    fun `spread author should have zero unread notifications after marking as read`() {
        loginAndSetToken(spreadAuthorUsername, "Test@123")

        val response = notificationClient.getUnreadCount()
        assertEquals(200, response.statusCode.value())
        assertEquals(0L, response.body!!.count)
    }

    @Test
    @Order(8)
    fun `medium cannot mark spread author's notification as read`() {
        loginAndSetToken(mediumUsername, "Test@456")

        assertThrowsWithStatus(403) {
            notificationClient.markAsRead(notificationId)
        }
    }

    @Test
    @Order(9)
    fun `medium adding interpretation to own spread should not create notification`() {
        loginAndSetToken(mediumUsername, "Test@456")

        // Medium creates their own spread
        val spreadRequest =
            CreateSpreadRequest(
                question = "Medium's own spread",
                layoutTypeId = oneCardLayoutId!!,
            )
        val spreadResponse = divinationClient.createSpread(spreadRequest)
        val mediumSpreadId = spreadResponse.body!!.id

        // Medium adds interpretation to their own spread
        val interpretationRequest =
            CreateInterpretationRequest(
                text = "Medium's own interpretation",
            )
        divinationClient.createInterpretation(mediumSpreadId, interpretationRequest)

        // Wait a bit for potential event processing
        Thread.sleep(2000)

        // Medium should have zero notifications (own interpretation doesn't trigger notification)
        val unreadResponse = notificationClient.getUnreadCount()
        assertEquals(200, unreadResponse.statusCode.value())
        assertEquals(0L, unreadResponse.body!!.count, "Should have 0 notifications for own interpretation")
    }

    @Test
    @Order(10)
    fun `mark all read should work correctly`() {
        loginAndSetToken(spreadAuthorUsername, "Test@123")

        // Medium adds another interpretation to create another notification
        loginAndSetToken(mediumUsername, "Test@456")
        val request =
            CreateInterpretationRequest(
                text = "Another interpretation to trigger notification",
            )
        // Create a new spread for this
        loginAndSetToken(spreadAuthorUsername, "Test@123")
        val spreadRequest =
            CreateSpreadRequest(
                question = "Another spread for mark-all-read test",
                layoutTypeId = oneCardLayoutId!!,
            )
        val spreadResponse = divinationClient.createSpread(spreadRequest)
        val anotherSpreadId = spreadResponse.body!!.id

        // Medium adds interpretation
        loginAndSetToken(mediumUsername, "Test@456")
        divinationClient.createInterpretation(anotherSpreadId, request)

        // Wait for notification
        loginAndSetToken(spreadAuthorUsername, "Test@123")
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted {
                val response = notificationClient.getUnreadCount()
                assertTrue(response.body!!.count >= 1L, "Should have at least 1 unread notification")
            }

        // Mark all as read
        val markAllResponse = notificationClient.markAllAsRead()
        assertEquals(200, markAllResponse.statusCode.value())

        // Verify all are read
        val afterResponse = notificationClient.getUnreadCount()
        assertEquals(0L, afterResponse.body!!.count, "Should have 0 unread after mark-all-read")
    }
}
