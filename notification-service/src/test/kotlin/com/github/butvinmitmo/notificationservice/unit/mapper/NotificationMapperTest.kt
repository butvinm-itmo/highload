package com.github.butvinmitmo.notificationservice.unit.mapper

import com.github.butvinmitmo.notificationservice.TestEntityFactory
import com.github.butvinmitmo.notificationservice.mapper.NotificationMapper
import com.github.butvinmitmo.shared.dto.NotificationType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class NotificationMapperTest {
    private val mapper = NotificationMapper()

    @Test
    fun `toDto should correctly map all fields from entity`() {
        val id = UUID.randomUUID()
        val spreadId = UUID.randomUUID()
        val interpretationId = UUID.randomUUID()
        val createdAt = Instant.now()

        val entity =
            TestEntityFactory.createNotification(
                id = id,
                userId = UUID.randomUUID(),
                type = "NEW_INTERPRETATION",
                title = "Test title",
                message = "Test message",
                isRead = true,
                spreadId = spreadId,
                interpretationId = interpretationId,
                createdAt = createdAt,
            )

        val dto = mapper.toDto(entity)

        assertEquals(id, dto.id)
        assertEquals(NotificationType.NEW_INTERPRETATION, dto.type)
        assertEquals("Test title", dto.title)
        assertEquals("Test message", dto.message)
        assertEquals(true, dto.isRead)
        assertEquals(createdAt, dto.createdAt)
        assertEquals(spreadId, dto.spreadId)
        assertEquals(interpretationId, dto.interpretationId)
    }

    @Test
    fun `toDto should correctly map NEW_INTERPRETATION type`() {
        val entity = TestEntityFactory.createNotification(type = "NEW_INTERPRETATION")

        val dto = mapper.toDto(entity)

        assertEquals(NotificationType.NEW_INTERPRETATION, dto.type)
    }

    @Test
    fun `toDto should correctly map NEW_SPREAD type`() {
        val spreadId = UUID.randomUUID()
        val entity =
            TestEntityFactory.createNotification(
                type = "NEW_SPREAD",
                spreadId = spreadId,
                interpretationId = null,
            )

        val dto = mapper.toDto(entity)

        assertEquals(NotificationType.NEW_SPREAD, dto.type)
        assertEquals(spreadId, dto.spreadId)
        assertEquals(null, dto.interpretationId)
    }

    @Test
    fun `toDto should correctly map notification with spreadId and interpretationId`() {
        val spreadId = UUID.randomUUID()
        val interpretationId = UUID.randomUUID()
        val entity =
            TestEntityFactory.createNotification(
                spreadId = spreadId,
                interpretationId = interpretationId,
            )

        val dto = mapper.toDto(entity)

        assertEquals(spreadId, dto.spreadId)
        assertEquals(interpretationId, dto.interpretationId)
    }

    @Test
    fun `toDto should correctly map notification with null spreadId`() {
        val interpretationId = UUID.randomUUID()
        val entity =
            TestEntityFactory.createNotification(
                spreadId = null,
                interpretationId = interpretationId,
            )

        val dto = mapper.toDto(entity)

        assertEquals(null, dto.spreadId)
        assertEquals(interpretationId, dto.interpretationId)
    }

    @Test
    fun `toDto should correctly map isRead false`() {
        val entity = TestEntityFactory.createNotification(isRead = false)

        val dto = mapper.toDto(entity)

        assertEquals(false, dto.isRead)
    }

    @Test
    fun `toDto should correctly map isRead true`() {
        val entity = TestEntityFactory.createNotification(isRead = true)

        val dto = mapper.toDto(entity)

        assertEquals(true, dto.isRead)
    }
}
