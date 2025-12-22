package com.github.butvinmitmo.notificationservice.integration.controller

import com.github.butvinmitmo.notificationservice.TestEntityFactory
import com.github.butvinmitmo.notificationservice.integration.BaseIntegrationTest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class NotificationControllerIntegrationTest : BaseIntegrationTest() {
    private val testUserId1 = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val testUserId2 = UUID.fromString("00000000-0000-0000-0000-000000000002")

    @Test
    fun `GET notifications should return empty list when no notifications`() {
        webTestClient
            .get()
            .uri("/api/v0.0.1/notifications")
            .header("X-User-Id", testUserId1.toString())
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .valueEquals("X-Total-Count", "0")
            .expectBody()
            .jsonPath("$")
            .isArray
            .jsonPath("$.length()")
            .isEqualTo(0)
    }

    @Test
    fun `GET notifications should return paginated notifications for user`() {
        // Insert 3 notifications for testUserId1
        val notification1 =
            TestEntityFactory.createNotification(
                id = null,
                userId = testUserId1,
                title = "Notification 1",
                createdAt = Instant.now(),
            )
        val notification2 =
            TestEntityFactory.createNotification(
                id = null,
                userId = testUserId1,
                title = "Notification 2",
                createdAt = Instant.now().minusSeconds(60),
            )
        val notification3 =
            TestEntityFactory.createNotification(
                id = null,
                userId = testUserId1,
                title = "Notification 3",
                createdAt = Instant.now().minusSeconds(120),
            )

        notificationRepository.save(notification1).block()
        notificationRepository.save(notification2).block()
        notificationRepository.save(notification3).block()

        // Request with page=0, size=2
        webTestClient
            .get()
            .uri("/api/v0.0.1/notifications?page=0&size=2")
            .header("X-User-Id", testUserId1.toString())
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .valueEquals("X-Total-Count", "3")
            .expectBody()
            .jsonPath("$.length()")
            .isEqualTo(2)
    }

    @Test
    fun `GET notifications should only return notifications for authenticated user`() {
        // Insert notifications for both users
        val notification1 =
            TestEntityFactory.createNotification(
                id = null,
                userId = testUserId1,
                title = "User 1 Notification",
            )
        val notification2 =
            TestEntityFactory.createNotification(
                id = null,
                userId = testUserId2,
                title = "User 2 Notification",
            )

        notificationRepository.save(notification1).block()
        notificationRepository.save(notification2).block()

        // Request as user 1
        webTestClient
            .get()
            .uri("/api/v0.0.1/notifications")
            .header("X-User-Id", testUserId1.toString())
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .valueEquals("X-Total-Count", "1")
            .expectBody()
            .jsonPath("$.length()")
            .isEqualTo(1)
            .jsonPath("$[0].title")
            .isEqualTo("User 1 Notification")
    }

    @Test
    fun `GET notifications should return notifications in descending order by createdAt`() {
        val olderNotification =
            TestEntityFactory.createNotification(
                id = null,
                userId = testUserId1,
                title = "Older",
                createdAt = Instant.now().minusSeconds(120),
            )
        val newerNotification =
            TestEntityFactory.createNotification(
                id = null,
                userId = testUserId1,
                title = "Newer",
                createdAt = Instant.now(),
            )

        notificationRepository.save(olderNotification).block()
        notificationRepository.save(newerNotification).block()

        webTestClient
            .get()
            .uri("/api/v0.0.1/notifications")
            .header("X-User-Id", testUserId1.toString())
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].title")
            .isEqualTo("Newer")
            .jsonPath("$[1].title")
            .isEqualTo("Older")
    }

    @Test
    fun `GET unread-count should return correct count`() {
        // Insert 2 unread and 1 read notification
        val unread1 =
            TestEntityFactory.createNotification(
                id = null,
                userId = testUserId1,
                isRead = false,
            )
        val unread2 =
            TestEntityFactory.createNotification(
                id = null,
                userId = testUserId1,
                isRead = false,
            )
        val read1 =
            TestEntityFactory.createNotification(
                id = null,
                userId = testUserId1,
                isRead = true,
            )

        notificationRepository.save(unread1).block()
        notificationRepository.save(unread2).block()
        notificationRepository.save(read1).block()

        webTestClient
            .get()
            .uri("/api/v0.0.1/notifications/unread-count")
            .header("X-User-Id", testUserId1.toString())
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.count")
            .isEqualTo(2)
    }

    @Test
    fun `GET unread-count should return zero when all notifications are read`() {
        val read1 =
            TestEntityFactory.createNotification(
                id = null,
                userId = testUserId1,
                isRead = true,
            )
        notificationRepository.save(read1).block()

        webTestClient
            .get()
            .uri("/api/v0.0.1/notifications/unread-count")
            .header("X-User-Id", testUserId1.toString())
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.count")
            .isEqualTo(0)
    }

    @Test
    fun `PATCH notifications id read should mark notification as read`() {
        val notification =
            TestEntityFactory.createNotification(
                id = null,
                userId = testUserId1,
                isRead = false,
            )
        val saved = notificationRepository.save(notification).block()!!

        webTestClient
            .patch()
            .uri("/api/v0.0.1/notifications/${saved.id}/read")
            .header("X-User-Id", testUserId1.toString())
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.isRead")
            .isEqualTo(true)
            .jsonPath("$.id")
            .isEqualTo(saved.id.toString())

        // Verify in database
        val updated = notificationRepository.findById(saved.id!!).block()!!
        assert(updated.isRead)
    }

    @Test
    fun `PATCH notifications id read should return 404 for non-existent notification`() {
        val randomId = UUID.randomUUID()

        webTestClient
            .patch()
            .uri("/api/v0.0.1/notifications/$randomId/read")
            .header("X-User-Id", testUserId1.toString())
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `PATCH notifications id read should return 403 when user is not owner`() {
        val notification =
            TestEntityFactory.createNotification(
                id = null,
                userId = testUserId1,
                isRead = false,
            )
        val saved = notificationRepository.save(notification).block()!!

        // Try to mark as read as different user
        webTestClient
            .patch()
            .uri("/api/v0.0.1/notifications/${saved.id}/read")
            .header("X-User-Id", testUserId2.toString())
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `POST mark-all-read should mark all notifications as read`() {
        val unread1 =
            TestEntityFactory.createNotification(
                id = null,
                userId = testUserId1,
                isRead = false,
            )
        val unread2 =
            TestEntityFactory.createNotification(
                id = null,
                userId = testUserId1,
                isRead = false,
            )
        val unread3 =
            TestEntityFactory.createNotification(
                id = null,
                userId = testUserId1,
                isRead = false,
            )

        notificationRepository.save(unread1).block()
        notificationRepository.save(unread2).block()
        notificationRepository.save(unread3).block()

        // Verify 3 unread before
        val beforeCount = notificationRepository.countUnreadByUserId(testUserId1).block()!!
        assert(beforeCount == 3L) { "Should have 3 unread notifications before" }

        webTestClient
            .post()
            .uri("/api/v0.0.1/notifications/mark-all-read")
            .header("X-User-Id", testUserId1.toString())
            .exchange()
            .expectStatus()
            .isOk

        // Verify all are read after
        val afterCount = notificationRepository.countUnreadByUserId(testUserId1).block()!!
        assert(afterCount == 0L) { "All notifications should be read after mark-all-read" }
    }

    @Test
    fun `POST mark-all-read should handle no unread notifications`() {
        val read1 =
            TestEntityFactory.createNotification(
                id = null,
                userId = testUserId1,
                isRead = true,
            )
        notificationRepository.save(read1).block()

        webTestClient
            .post()
            .uri("/api/v0.0.1/notifications/mark-all-read")
            .header("X-User-Id", testUserId1.toString())
            .exchange()
            .expectStatus()
            .isOk

        // Verify no change
        val count = notificationRepository.countByUserId(testUserId1).block()!!
        assert(count == 1L) { "Should still have 1 notification" }
    }

    @Test
    fun `GET notifications with invalid size should return error`() {
        // Validation for query parameters may return different error codes in WebFlux
        webTestClient
            .get()
            .uri("/api/v0.0.1/notifications?size=100")
            .header("X-User-Id", testUserId1.toString())
            .exchange()
            .expectStatus()
            .is5xxServerError // WebFlux constraint validation on query params throws exception
    }

    @Test
    fun `GET notifications with negative page should return error`() {
        // Validation for query parameters may return different error codes in WebFlux
        webTestClient
            .get()
            .uri("/api/v0.0.1/notifications?page=-1")
            .header("X-User-Id", testUserId1.toString())
            .exchange()
            .expectStatus()
            .is5xxServerError // WebFlux constraint validation on query params throws exception
    }
}
