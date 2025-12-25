package com.github.butvinmitmo.notificationservice.unit.mapper

import com.github.butvinmitmo.notificationservice.TestEntityFactory
import com.github.butvinmitmo.notificationservice.mapper.NotificationMapper
import com.github.butvinmitmo.shared.dto.NotificationType
import com.github.butvinmitmo.shared.dto.ReferenceType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class NotificationMapperTest {
    private val mapper = NotificationMapper()

    @Test
    fun `toDto should correctly map all fields from entity`() {
        val id = UUID.randomUUID()
        val referenceId = UUID.randomUUID()
        val createdAt = Instant.now()

        val entity =
            TestEntityFactory.createNotification(
                id = id,
                userId = UUID.randomUUID(),
                type = "NEW_INTERPRETATION",
                title = "Test title",
                message = "Test message",
                isRead = true,
                referenceId = referenceId,
                referenceType = "INTERPRETATION",
                createdAt = createdAt,
            )

        val dto = mapper.toDto(entity)

        assertEquals(id, dto.id)
        assertEquals(NotificationType.NEW_INTERPRETATION, dto.type)
        assertEquals("Test title", dto.title)
        assertEquals("Test message", dto.message)
        assertEquals(true, dto.isRead)
        assertEquals(createdAt, dto.createdAt)
        assertEquals(referenceId, dto.referenceId)
        assertEquals(ReferenceType.INTERPRETATION, dto.referenceType)
    }

    @Test
    fun `toDto should correctly map NEW_INTERPRETATION type`() {
        val entity = TestEntityFactory.createNotification(type = "NEW_INTERPRETATION")

        val dto = mapper.toDto(entity)

        assertEquals(NotificationType.NEW_INTERPRETATION, dto.type)
    }

    @Test
    fun `toDto should correctly map NEW_SPREAD type`() {
        val entity =
            TestEntityFactory.createNotification(
                type = "NEW_SPREAD",
                referenceType = "SPREAD",
            )

        val dto = mapper.toDto(entity)

        assertEquals(NotificationType.NEW_SPREAD, dto.type)
    }

    @Test
    fun `toDto should correctly map INTERPRETATION reference type`() {
        val entity = TestEntityFactory.createNotification(referenceType = "INTERPRETATION")

        val dto = mapper.toDto(entity)

        assertEquals(ReferenceType.INTERPRETATION, dto.referenceType)
    }

    @Test
    fun `toDto should correctly map SPREAD reference type`() {
        val entity =
            TestEntityFactory.createNotification(
                type = "NEW_SPREAD",
                referenceType = "SPREAD",
            )

        val dto = mapper.toDto(entity)

        assertEquals(ReferenceType.SPREAD, dto.referenceType)
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
