package com.github.butvinmitmo.divinationservice.infrastructure.messaging

import com.github.butvinmitmo.divinationservice.application.service.DivinationService
import com.github.butvinmitmo.shared.dto.events.EventType
import com.github.butvinmitmo.shared.dto.events.UserEventData
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class UserEventConsumer(
    private val divinationService: DivinationService,
) {
    private val logger = LoggerFactory.getLogger(UserEventConsumer::class.java)

    @KafkaListener(
        topics = ["\${kafka.topics.users-events:users-events}"],
        containerFactory = "userKafkaListenerContainerFactory",
    )
    fun onUserEvent(
        @Payload event: UserEventData,
        @Header("eventType") eventTypeBytes: ByteArray,
    ) {
        val eventType =
            try {
                EventType.valueOf(String(eventTypeBytes))
            } catch (e: IllegalArgumentException) {
                logger.warn("Unknown event type: {}, skipping", String(eventTypeBytes))
                return
            }

        // Only process DELETED events for user data cleanup
        if (eventType != EventType.DELETED) {
            logger.debug("Ignoring {} event for user {}", eventType, event.id)
            return
        }

        logger.info("Processing DELETED event for user {}", event.id)

        divinationService
            .deleteUserData(event.id)
            .doOnSuccess {
                logger.info("Successfully deleted all data for user {}", event.id)
            }.doOnError { e ->
                logger.error("Failed to delete data for user {}: {}", event.id, e.message)
            }.subscribe()
    }
}
