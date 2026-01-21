package com.github.butvinmitmo.divinationservice.unit.messaging

import com.github.butvinmitmo.divinationservice.application.service.DivinationService
import com.github.butvinmitmo.divinationservice.infrastructure.messaging.UserEventConsumer
import com.github.butvinmitmo.shared.dto.events.EventType
import com.github.butvinmitmo.shared.dto.events.UserEventData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserEventConsumerTest {
    @Mock
    private lateinit var divinationService: DivinationService

    private lateinit var userEventConsumer: UserEventConsumer

    private val userId = UUID.randomUUID()
    private val testUserEvent =
        UserEventData(
            id = userId,
            username = "testuser",
            role = "USER",
            createdAt = Instant.now(),
        )

    @BeforeEach
    fun setup() {
        userEventConsumer = UserEventConsumer(divinationService)
    }

    @Test
    fun `onUserEvent should call deleteUserData for DELETED event`() {
        whenever(divinationService.deleteUserData(userId)).thenReturn(Mono.empty())

        userEventConsumer.onUserEvent(
            event = testUserEvent,
            eventTypeBytes = EventType.DELETED.name.toByteArray(),
        )

        verify(divinationService).deleteUserData(userId)
    }

    @Test
    fun `onUserEvent should ignore CREATED event`() {
        userEventConsumer.onUserEvent(
            event = testUserEvent,
            eventTypeBytes = EventType.CREATED.name.toByteArray(),
        )

        verify(divinationService, never()).deleteUserData(any())
    }

    @Test
    fun `onUserEvent should ignore UPDATED event`() {
        userEventConsumer.onUserEvent(
            event = testUserEvent,
            eventTypeBytes = EventType.UPDATED.name.toByteArray(),
        )

        verify(divinationService, never()).deleteUserData(any())
    }

    @Test
    fun `onUserEvent should handle unknown event type gracefully`() {
        userEventConsumer.onUserEvent(
            event = testUserEvent,
            eventTypeBytes = "UNKNOWN_EVENT".toByteArray(),
        )

        verify(divinationService, never()).deleteUserData(any())
    }
}
