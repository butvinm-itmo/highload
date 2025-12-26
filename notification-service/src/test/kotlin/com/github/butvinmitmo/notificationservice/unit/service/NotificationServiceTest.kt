package com.github.butvinmitmo.notificationservice.unit.service

import com.github.butvinmitmo.notificationservice.TestEntityFactory
import com.github.butvinmitmo.notificationservice.exception.ForbiddenException
import com.github.butvinmitmo.notificationservice.exception.NotFoundException
import com.github.butvinmitmo.notificationservice.mapper.NotificationMapper
import com.github.butvinmitmo.notificationservice.repository.NotificationRepository
import com.github.butvinmitmo.notificationservice.service.NotificationService
import com.github.butvinmitmo.shared.dto.NotificationDto
import com.github.butvinmitmo.shared.dto.NotificationType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class NotificationServiceTest {
    @Mock
    private lateinit var notificationRepository: NotificationRepository

    @Mock
    private lateinit var notificationMapper: NotificationMapper

    private lateinit var notificationService: NotificationService

    private val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val otherUserId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val notificationId = UUID.randomUUID()
    private val spreadId = UUID.randomUUID()
    private val interpretationId = UUID.randomUUID()
    private val createdAt = Instant.now()

    @BeforeEach
    fun setup() {
        notificationService = NotificationService(notificationRepository, notificationMapper)
    }

    @Test
    fun `getNotifications should return paginated notifications`() {
        val notifications =
            listOf(
                TestEntityFactory.createNotification(id = UUID.randomUUID(), userId = userId, createdAt = createdAt),
                TestEntityFactory.createNotification(
                    id = UUID.randomUUID(),
                    userId = userId,
                    createdAt = createdAt.minusSeconds(60),
                ),
            )

        val dtos =
            notifications.map { notification ->
                NotificationDto(
                    id = notification.id!!,
                    type = NotificationType.NEW_INTERPRETATION,
                    title = notification.title,
                    message = notification.message,
                    isRead = notification.isRead,
                    createdAt = notification.createdAt!!,
                    spreadId = notification.spreadId,
                    interpretationId = notification.interpretationId,
                )
            }

        whenever(notificationRepository.countByUserId(userId)).thenReturn(Mono.just(2L))
        whenever(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, 0L, 10))
            .thenReturn(Flux.fromIterable(notifications))
        whenever(notificationMapper.toDto(notifications[0])).thenReturn(dtos[0])
        whenever(notificationMapper.toDto(notifications[1])).thenReturn(dtos[1])

        val result = notificationService.getNotifications(userId, 0, 10).block()

        assertNotNull(result)
        assertEquals(2, result!!.content.size)
        assertEquals(2L, result.totalElements)
        assertEquals(1, result.totalPages)
        assertEquals(0, result.page)
        assertEquals(10, result.size)
        assertTrue(result.isFirst)
        assertTrue(result.isLast)
    }

    @Test
    fun `getNotifications should return empty page when no notifications`() {
        whenever(notificationRepository.countByUserId(userId)).thenReturn(Mono.just(0L))
        whenever(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, 0L, 10))
            .thenReturn(Flux.empty())

        val result = notificationService.getNotifications(userId, 0, 10).block()

        assertNotNull(result)
        assertEquals(0, result!!.content.size)
        assertEquals(0L, result.totalElements)
        assertEquals(0, result.totalPages)
        assertTrue(result.isFirst)
        assertTrue(result.isLast)
    }

    @Test
    fun `getNotifications should calculate pagination correctly for multiple pages`() {
        val notifications =
            listOf(
                TestEntityFactory.createNotification(id = UUID.randomUUID(), userId = userId, createdAt = createdAt),
            )

        val dto =
            NotificationDto(
                id = notifications[0].id!!,
                type = NotificationType.NEW_INTERPRETATION,
                title = notifications[0].title,
                message = notifications[0].message,
                isRead = notifications[0].isRead,
                createdAt = notifications[0].createdAt!!,
                spreadId = notifications[0].spreadId,
                interpretationId = notifications[0].interpretationId,
            )

        whenever(notificationRepository.countByUserId(userId)).thenReturn(Mono.just(25L))
        whenever(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, 10L, 10))
            .thenReturn(Flux.fromIterable(notifications))
        whenever(notificationMapper.toDto(notifications[0])).thenReturn(dto)

        val result = notificationService.getNotifications(userId, 1, 10).block()

        assertNotNull(result)
        assertEquals(25L, result!!.totalElements)
        assertEquals(3, result.totalPages)
        assertEquals(1, result.page)
        assertFalse(result.isFirst)
        assertFalse(result.isLast)
    }

    @Test
    fun `getUnreadCount should return count of unread notifications`() {
        whenever(notificationRepository.countUnreadByUserId(userId)).thenReturn(Mono.just(5L))

        val result = notificationService.getUnreadCount(userId).block()

        assertNotNull(result)
        assertEquals(5L, result!!.count)
    }

    @Test
    fun `getUnreadCount should return zero when no unread notifications`() {
        whenever(notificationRepository.countUnreadByUserId(userId)).thenReturn(Mono.just(0L))

        val result = notificationService.getUnreadCount(userId).block()

        assertNotNull(result)
        assertEquals(0L, result!!.count)
    }

    @Test
    fun `markAsRead should mark notification as read when user is owner`() {
        val notification =
            TestEntityFactory.createNotification(
                id = notificationId,
                userId = userId,
                isRead = false,
                spreadId = spreadId,
                interpretationId = interpretationId,
                createdAt = createdAt,
            )

        val savedNotification = notification.copy().apply { isRead = true }

        val dto =
            NotificationDto(
                id = notificationId,
                type = NotificationType.NEW_INTERPRETATION,
                title = notification.title,
                message = notification.message,
                isRead = true,
                createdAt = createdAt,
                spreadId = spreadId,
                interpretationId = interpretationId,
            )

        whenever(notificationRepository.findById(notificationId)).thenReturn(Mono.just(notification))
        whenever(notificationRepository.save(any())).thenReturn(Mono.just(savedNotification))
        whenever(notificationMapper.toDto(savedNotification)).thenReturn(dto)

        val result = notificationService.markAsRead(notificationId, userId).block()

        assertNotNull(result)
        assertTrue(result!!.isRead)
    }

    @Test
    fun `markAsRead should throw NotFoundException when notification not found`() {
        whenever(notificationRepository.findById(notificationId)).thenReturn(Mono.empty())

        val exception =
            assertThrows<NotFoundException> {
                notificationService.markAsRead(notificationId, userId).block()
            }
        assertEquals("Notification not found", exception.message)
    }

    @Test
    fun `markAsRead should throw ForbiddenException when user is not owner`() {
        val notification =
            TestEntityFactory.createNotification(
                id = notificationId,
                userId = otherUserId,
                isRead = false,
                createdAt = createdAt,
            )

        whenever(notificationRepository.findById(notificationId)).thenReturn(Mono.just(notification))

        val exception =
            assertThrows<ForbiddenException> {
                notificationService.markAsRead(notificationId, userId).block()
            }
        assertEquals("You can only mark your own notifications as read", exception.message)
    }

    @Test
    fun `markAllAsRead should return count of marked notifications`() {
        whenever(notificationRepository.markAllAsReadByUserId(userId)).thenReturn(Mono.just(3L))

        val result = notificationService.markAllAsRead(userId).block()

        assertNotNull(result)
        assertEquals(3L, result!!.markedAsRead)
    }

    @Test
    fun `markAllAsRead should return zero when no unread notifications`() {
        whenever(notificationRepository.markAllAsReadByUserId(userId)).thenReturn(Mono.just(0L))

        val result = notificationService.markAllAsRead(userId).block()

        assertNotNull(result)
        assertEquals(0L, result!!.markedAsRead)
    }
}
