package com.github.butvinmitmo.notificationservice.integration.controller

import com.github.butvinmitmo.notificationservice.application.interfaces.provider.SpreadProvider
import com.github.butvinmitmo.notificationservice.infrastructure.persistence.entity.NotificationEntity
import com.github.butvinmitmo.notificationservice.infrastructure.persistence.repository.SpringDataNotificationRepository
import com.github.butvinmitmo.notificationservice.infrastructure.websocket.WebSocketSessionManager
import com.github.butvinmitmo.shared.dto.NotificationDto
import com.github.butvinmitmo.shared.dto.UnreadCountResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Instant
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class NotificationControllerIntegrationTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var springDataNotificationRepository: SpringDataNotificationRepository

    @MockBean
    private lateinit var spreadProvider: SpreadProvider

    @MockBean
    private lateinit var webSocketSessionManager: WebSocketSessionManager

    private val testUserId = UUID.randomUUID()
    private val otherUserId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        // Create test notifications
        val notifications =
            listOf(
                createNotificationEntity(testUserId, false),
                createNotificationEntity(testUserId, false),
                createNotificationEntity(testUserId, true),
                createNotificationEntity(otherUserId, false),
            )
        notifications.forEach { springDataNotificationRepository.save(it).block() }
    }

    @AfterEach
    fun cleanup() {
        springDataNotificationRepository.deleteAll().block()
    }

    private fun createNotificationEntity(
        recipientId: UUID,
        isRead: Boolean,
    ): NotificationEntity =
        NotificationEntity(
            id = null,
            recipientId = recipientId,
            interpretationId = UUID.randomUUID(),
            interpretationAuthorId = UUID.randomUUID(),
            spreadId = UUID.randomUUID(),
            title = "Test notification",
            message = "Test message",
            isRead = isRead,
            createdAt = Instant.now(),
        )

    @Test
    fun `getNotifications should return paginated notifications for user`() {
        webTestClient
            .get()
            .uri("/api/v0.0.1/notifications?page=0&size=10")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .valueEquals("X-Total-Count", "3")
            .expectBodyList(NotificationDto::class.java)
            .hasSize(3)
    }

    @Test
    fun `getNotifications should filter by isRead status`() {
        webTestClient
            .get()
            .uri("/api/v0.0.1/notifications?isRead=false&page=0&size=10")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .valueEquals("X-Total-Count", "2")
            .expectBodyList(NotificationDto::class.java)
            .hasSize(2)
    }

    @Test
    fun `getUnreadCount should return unread count for user`() {
        webTestClient
            .get()
            .uri("/api/v0.0.1/notifications/unread-count")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UnreadCountResponse::class.java)
            .consumeWith { response ->
                assert(response.responseBody?.count == 2L)
            }
    }

    @Test
    fun `markAsRead should mark notification as read`() {
        // Get an unread notification ID for this user
        val notification =
            springDataNotificationRepository
                .findByRecipientIdAndIsReadPaginated(testUserId, false, 0, 10)
                .blockFirst()!!

        webTestClient
            .put()
            .uri("/api/v0.0.1/notifications/${notification.id}/read")
            .header("X-User-Id", testUserId.toString())
            .header("X-User-Role", "USER")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(NotificationDto::class.java)
            .consumeWith { response ->
                assert(response.responseBody?.isRead == true)
            }
    }

    @Test
    fun `getNotifications should return 401 without auth headers`() {
        webTestClient
            .get()
            .uri("/api/v0.0.1/notifications")
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    companion object {
        @JvmStatic
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer("postgres:15-alpine")
                .withDatabaseName("tarot_db_test")
                .withUsername("test_user")
                .withPassword("test_password")
                .apply {
                    start()
                }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
            registry.add("spring.flyway.url") { postgres.jdbcUrl }
            registry.add("spring.flyway.user") { postgres.username }
            registry.add("spring.flyway.password") { postgres.password }
            registry.add("spring.flyway.enabled") { "true" }
            // Disable Kafka for tests
            registry.add("spring.kafka.bootstrap-servers") { "localhost:9092" }
            registry.add("spring.kafka.listener.auto-startup") { "false" }
        }
    }
}
