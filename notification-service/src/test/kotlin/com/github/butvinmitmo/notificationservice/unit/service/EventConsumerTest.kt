package com.github.butvinmitmo.notificationservice.unit.service

import com.github.butvinmitmo.notificationservice.entity.Notification
import com.github.butvinmitmo.notificationservice.mapper.NotificationMapper
import com.github.butvinmitmo.notificationservice.repository.NotificationRepository
import com.github.butvinmitmo.notificationservice.service.EventConsumer
import com.github.butvinmitmo.notificationservice.websocket.NotificationBroadcaster
import com.github.butvinmitmo.shared.dto.InterpretationCreatedEvent
import com.github.butvinmitmo.shared.dto.NotificationDto
import com.github.butvinmitmo.shared.dto.SpreadCreatedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.receiver.KafkaReceiver
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class EventConsumerTest {
    @Mock
    private lateinit var spreadEventReceiver: KafkaReceiver<String, SpreadCreatedEvent>

    @Mock
    private lateinit var interpretationEventReceiver: KafkaReceiver<String, InterpretationCreatedEvent>

    @Mock
    private lateinit var notificationRepository: NotificationRepository

    @Mock
    private lateinit var notificationBroadcaster: NotificationBroadcaster

    @Mock
    private lateinit var notificationMapper: NotificationMapper

    @Captor
    private lateinit var notificationCaptor: ArgumentCaptor<Notification>

    private lateinit var eventConsumer: EventConsumer

    private val spreadAuthorId = UUID.randomUUID()
    private val interpretationAuthorId = UUID.randomUUID()
    private val spreadId = UUID.randomUUID()
    private val interpretationId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        Mockito.lenient().`when`(spreadEventReceiver.receive()).thenReturn(Flux.never())
        Mockito.lenient().`when`(interpretationEventReceiver.receive()).thenReturn(Flux.never())
        Mockito.lenient().`when`(notificationBroadcaster.broadcast(any(), any())).thenReturn(Mono.empty())
        Mockito.lenient().`when`(notificationMapper.toDto(any())).thenReturn(
            Mockito.mock(NotificationDto::class.java),
        )

        eventConsumer =
            EventConsumer(
                spreadEventReceiver,
                interpretationEventReceiver,
                notificationRepository,
                notificationBroadcaster,
                notificationMapper,
            )
    }

    @Test
    fun `handleInterpretationCreatedEvent should create notification for spread author`() {
        val event =
            InterpretationCreatedEvent(
                eventId = UUID.randomUUID(),
                timestamp = Instant.now(),
                interpretationId = interpretationId,
                spreadId = spreadId,
                spreadAuthorId = spreadAuthorId,
                interpretationAuthorId = interpretationAuthorId,
                interpretationAuthorUsername = "medium_user",
                textPreview = "This is a test interpretation",
            )

        val savedNotification =
            Notification(
                id = UUID.randomUUID(),
                userId = spreadAuthorId,
                type = "NEW_INTERPRETATION",
                title = "New interpretation on your spread",
                message = "medium_user added an interpretation: \"This is a test interpretation...\"",
                referenceId = interpretationId,
                referenceType = "INTERPRETATION",
                createdAt = Instant.now(),
            )

        whenever(notificationRepository.save(any())).thenReturn(Mono.just(savedNotification))

        // Use reflection to call the private method
        val handleMethod =
            EventConsumer::class.java.getDeclaredMethod(
                "handleInterpretationCreatedEvent",
                InterpretationCreatedEvent::class.java,
            )
        handleMethod.isAccessible = true
        val result = handleMethod.invoke(eventConsumer, event) as Mono<*>
        result.block()

        verify(notificationRepository).save(notificationCaptor.capture())

        val capturedNotification = notificationCaptor.value
        assertEquals(spreadAuthorId, capturedNotification.userId)
        assertEquals("NEW_INTERPRETATION", capturedNotification.type)
        assertEquals("New interpretation on your spread", capturedNotification.title)
        assertEquals(
            "medium_user added an interpretation: \"This is a test interpretation...\"",
            capturedNotification.message,
        )
        assertEquals(interpretationId, capturedNotification.referenceId)
        assertEquals("INTERPRETATION", capturedNotification.referenceType)
        assertEquals(false, capturedNotification.isRead)
    }

    @Test
    fun `handleInterpretationCreatedEvent should NOT create notification when author is spread owner`() {
        val sameUserId = UUID.randomUUID()
        val event =
            InterpretationCreatedEvent(
                eventId = UUID.randomUUID(),
                timestamp = Instant.now(),
                interpretationId = interpretationId,
                spreadId = spreadId,
                spreadAuthorId = sameUserId,
                interpretationAuthorId = sameUserId, // Same as spread author
                interpretationAuthorUsername = "user",
                textPreview = "Self interpretation",
            )

        // Use reflection to call the private method
        val handleMethod =
            EventConsumer::class.java.getDeclaredMethod(
                "handleInterpretationCreatedEvent",
                InterpretationCreatedEvent::class.java,
            )
        handleMethod.isAccessible = true
        val result = handleMethod.invoke(eventConsumer, event) as Mono<*>
        result.block()

        verify(notificationRepository, never()).save(any())
    }

    @Test
    fun `handleSpreadCreatedEvent should be a no-op`() {
        val event =
            SpreadCreatedEvent(
                eventId = UUID.randomUUID(),
                timestamp = Instant.now(),
                spreadId = spreadId,
                authorId = spreadAuthorId,
                authorUsername = "user",
                question = "What does the future hold?",
                layoutTypeName = "THREE_CARDS",
                cardsCount = 3,
            )

        // Use reflection to call the private method
        val handleMethod =
            EventConsumer::class.java.getDeclaredMethod(
                "handleSpreadCreatedEvent",
                SpreadCreatedEvent::class.java,
            )
        handleMethod.isAccessible = true
        val result = handleMethod.invoke(eventConsumer, event) as Mono<*>
        result.block()

        verify(notificationRepository, never()).save(any())
    }
}
