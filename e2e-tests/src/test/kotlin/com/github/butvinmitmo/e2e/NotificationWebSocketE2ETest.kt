package com.github.butvinmitmo.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.butvinmitmo.e2e.config.AuthContext
import com.github.butvinmitmo.shared.client.NotificationServiceClient
import com.github.butvinmitmo.shared.dto.CreateInterpretationRequest
import com.github.butvinmitmo.shared.dto.CreateSpreadRequest
import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.NotificationDto
import com.github.butvinmitmo.shared.dto.NotificationType
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.net.URI
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class NotificationWebSocketE2ETest : BaseE2ETest() {
    @Autowired
    protected lateinit var notificationClient: NotificationServiceClient

    companion object {
        private lateinit var spreadAuthorId: UUID
        private lateinit var spreadAuthorUsername: String
        private lateinit var spreadAuthorToken: String
        private lateinit var mediumUserId: UUID
        private lateinit var mediumUsername: String
        private lateinit var spreadId: UUID
        private var oneCardLayoutId: UUID? = null

        private val objectMapper =
            ObjectMapper()
                .registerKotlinModule()
                .registerModule(JavaTimeModule())

        private val gatewayUrl: String
            get() =
                System.getProperty("GATEWAY_URL")
                    ?: System.getenv("GATEWAY_URL")
                    ?: "http://localhost:8080"

        private val gatewayWsUrl: String
            get() = gatewayUrl.replace("http://", "ws://").replace("https://", "wss://")
    }

    @BeforeAll
    fun setupTestData() {
        loginAsAdmin()

        spreadAuthorUsername = "e2e_ws_author_${System.currentTimeMillis()}"
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

        mediumUsername = "e2e_ws_medium_${System.currentTimeMillis()}"
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

        val layoutTypes = tarotClient.getLayoutTypes(currentUserId, currentRole).body!!
        oneCardLayoutId = layoutTypes.find { it.name == "ONE_CARD" }?.id

        spreadAuthorToken = loginAndSetToken(spreadAuthorUsername, "Test@123")
        val spreadRequest =
            CreateSpreadRequest(
                question = "E2E WebSocket test - What does the future hold?",
                layoutTypeId = oneCardLayoutId!!,
            )
        val spreadResponse = divinationClient.createSpread(spreadRequest)
        spreadId = spreadResponse.body!!.id
    }

    @AfterAll
    fun cleanup() {
        loginAsAdmin()
        runCatching { userClient.deleteUser(currentUserId, currentRole, spreadAuthorId) }
        runCatching { userClient.deleteUser(currentUserId, currentRole, mediumUserId) }
    }

    @Test
    @Order(1)
    fun `WebSocket connection should be established with valid JWT`() {
        loginAndSetToken(spreadAuthorUsername, "Test@123")
        val token = AuthContext.getToken()
        assertNotNull(token)

        val latch = CountDownLatch(1)
        var connected = false

        val client = StandardWebSocketClient()
        val wsUrl = "$gatewayWsUrl/api/v0.0.1/notifications/ws"
        val headers = WebSocketHttpHeaders()
        headers.add("Authorization", "Bearer $token")

        try {
            val session =
                client
                    .execute(
                        object : TextWebSocketHandler() {
                            override fun afterConnectionEstablished(session: WebSocketSession) {
                                connected = true
                                latch.countDown()
                            }
                        },
                        headers,
                        URI.create(wsUrl),
                    ).get(10, TimeUnit.SECONDS)

            latch.await(5, TimeUnit.SECONDS)
            assertTrue(connected, "WebSocket connection should be established")
            session.close()
        } catch (e: Exception) {
            throw AssertionError("Failed to establish WebSocket connection: ${e.message}", e)
        }
    }

    @Test
    @Order(2)
    fun `should receive notification via WebSocket when interpretation is added`() {
        loginAndSetToken(spreadAuthorUsername, "Test@123")
        val token = AuthContext.getToken()!!

        val receivedMessages = CopyOnWriteArrayList<NotificationDto>()
        val messageLatch = CountDownLatch(1)

        val client = StandardWebSocketClient()
        val wsUrl = "$gatewayWsUrl/api/v0.0.1/notifications/ws"
        val headers = WebSocketHttpHeaders()
        headers.add("Authorization", "Bearer $token")

        val session =
            client
                .execute(
                    object : TextWebSocketHandler() {
                        override fun handleTextMessage(
                            session: WebSocketSession,
                            message: TextMessage,
                        ) {
                            val notification = objectMapper.readValue(message.payload, NotificationDto::class.java)
                            receivedMessages.add(notification)
                            messageLatch.countDown()
                        }
                    },
                    headers,
                    URI.create(wsUrl),
                ).get(10, TimeUnit.SECONDS)

        try {
            Thread.sleep(1000)

            loginAndSetToken(mediumUsername, "Test@456")
            val interpretationRequest =
                CreateInterpretationRequest(
                    text = "WebSocket E2E test interpretation - The cards reveal clarity!",
                )
            divinationClient.createInterpretation(spreadId, interpretationRequest)

            val received = messageLatch.await(15, TimeUnit.SECONDS)
            assertTrue(received, "Should receive notification via WebSocket")
            assertEquals(1, receivedMessages.size)

            val notification = receivedMessages[0]
            assertEquals(NotificationType.NEW_INTERPRETATION, notification.type)
            assertEquals(spreadId, notification.spreadId)
            assertNotNull(notification.interpretationId)
            assertTrue(notification.message.contains(mediumUsername))
        } finally {
            session.close()
        }
    }

    @Test
    @Order(3)
    fun `notification should also be persisted and available via REST API`() {
        loginAndSetToken(spreadAuthorUsername, "Test@123")

        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted {
                val response = notificationClient.getNotifications()
                assertEquals(200, response.statusCode.value())

                val notifications = response.body!!
                assertTrue(notifications.isNotEmpty(), "Should have at least one notification")

                val wsNotification = notifications.find { it.message.contains("WebSocket E2E test interpretation") }
                assertNotNull(wsNotification, "Should find the notification from WebSocket test")
            }
    }

    @Test
    @Order(4)
    fun `WebSocket connection should be rejected without valid JWT`() {
        AuthContext.clear()

        val client = StandardWebSocketClient()
        val wsUrl = "$gatewayWsUrl/api/v0.0.1/notifications/ws"

        var connectionFailed = false
        try {
            client
                .execute(
                    object : TextWebSocketHandler() {},
                    WebSocketHttpHeaders(),
                    URI.create(wsUrl),
                ).get(5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            connectionFailed = true
        }

        assertTrue(connectionFailed, "WebSocket connection without JWT should be rejected")
    }
}
