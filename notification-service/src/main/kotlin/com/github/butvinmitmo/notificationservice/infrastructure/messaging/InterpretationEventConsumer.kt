package com.github.butvinmitmo.notificationservice.infrastructure.messaging

import com.github.butvinmitmo.notificationservice.application.interfaces.provider.SpreadProvider
import com.github.butvinmitmo.notificationservice.application.service.NotificationService
import com.github.butvinmitmo.shared.dto.events.EventType
import com.github.butvinmitmo.shared.dto.events.InterpretationEventData
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class InterpretationEventConsumer(
    private val spreadProvider: SpreadProvider,
    private val notificationService: NotificationService,
) {
    private val logger = LoggerFactory.getLogger(InterpretationEventConsumer::class.java)

    @KafkaListener(
        topics = ["\${kafka.topics.interpretations-events:interpretations-events}"],
        containerFactory = "interpretationKafkaListenerContainerFactory",
    )
    fun onInterpretationEvent(
        @Payload event: InterpretationEventData,
        @Header("eventType") eventTypeBytes: ByteArray,
    ) {
        val eventType =
            try {
                EventType.valueOf(String(eventTypeBytes))
            } catch (e: IllegalArgumentException) {
                logger.warn("Unknown event type: {}, skipping", String(eventTypeBytes))
                return
            }

        // Only process CREATED events for notifications
        if (eventType != EventType.CREATED) {
            logger.debug("Ignoring {} event for interpretation {}", eventType, event.id)
            return
        }

        logger.info(
            "Processing CREATED event for interpretation {} on spread {}",
            event.id,
            event.spreadId,
        )

        spreadProvider
            .getSpreadOwnerId(event.spreadId)
            .flatMap { spreadOwnerId ->
                // Don't notify if the interpretation author is the spread owner
                if (event.authorId == spreadOwnerId) {
                    logger.debug(
                        "Interpretation author {} is spread owner, skipping notification",
                        event.authorId,
                    )
                    return@flatMap reactor.core.publisher.Mono
                        .empty()
                }

                logger.info(
                    "Creating notification for spread owner {} about interpretation by {}",
                    spreadOwnerId,
                    event.authorId,
                )

                notificationService.create(
                    recipientId = spreadOwnerId,
                    interpretationId = event.id,
                    interpretationAuthorId = event.authorId,
                    spreadId = event.spreadId,
                    title = "New interpretation on your spread",
                    message = "Someone added an interpretation to your spread",
                )
            }.doOnSuccess { notification ->
                if (notification != null) {
                    logger.info("Created notification {} for interpretation {}", notification.id, event.id)
                }
            }.doOnError { e ->
                logger.error(
                    "Failed to process interpretation event {}: {}",
                    event.id,
                    e.message,
                )
            }.subscribe()
    }
}
