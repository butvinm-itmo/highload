package com.github.butvinmitmo.notificationservice.service

import com.github.butvinmitmo.notificationservice.entity.Notification
import com.github.butvinmitmo.notificationservice.mapper.NotificationMapper
import com.github.butvinmitmo.notificationservice.repository.NotificationRepository
import com.github.butvinmitmo.notificationservice.websocket.NotificationBroadcaster
import com.github.butvinmitmo.shared.dto.InterpretationCreatedEvent
import com.github.butvinmitmo.shared.dto.SpreadCreatedEvent
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.kafka.receiver.KafkaReceiver
import reactor.util.retry.Retry
import java.time.Duration

@Service
class EventConsumer(
    private val spreadEventReceiver: KafkaReceiver<String, SpreadCreatedEvent>,
    private val interpretationEventReceiver: KafkaReceiver<String, InterpretationCreatedEvent>,
    private val notificationRepository: NotificationRepository,
    private val notificationBroadcaster: NotificationBroadcaster,
    private val notificationMapper: NotificationMapper,
) {
    private val logger = LoggerFactory.getLogger(EventConsumer::class.java)

    private var spreadSubscription: Disposable? = null
    private var interpretationSubscription: Disposable? = null

    @PostConstruct
    fun startConsuming() {
        spreadSubscription =
            spreadEventReceiver
                .receive()
                .flatMap { record ->
                    handleSpreadCreatedEvent(record.value())
                        .doOnSuccess { record.receiverOffset().acknowledge() }
                        .doOnError { error ->
                            logger.error(
                                "Error processing SpreadCreatedEvent: ${record.value().spreadId}",
                                error,
                            )
                        }.onErrorResume { Mono.empty() }
                }.retryWhen(
                    Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1)).maxBackoff(Duration.ofMinutes(1)),
                ).subscribe()

        interpretationSubscription =
            interpretationEventReceiver
                .receive()
                .flatMap { record ->
                    handleInterpretationCreatedEvent(record.value())
                        .doOnSuccess { record.receiverOffset().acknowledge() }
                        .doOnError { error ->
                            logger.error(
                                "Error processing InterpretationCreatedEvent: ${record.value().interpretationId}",
                                error,
                            )
                        }.onErrorResume { Mono.empty() }
                }.retryWhen(
                    Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1)).maxBackoff(Duration.ofMinutes(1)),
                ).subscribe()

        logger.info("Kafka consumers started")
    }

    @PreDestroy
    fun stopConsuming() {
        spreadSubscription?.dispose()
        interpretationSubscription?.dispose()
        logger.info("Kafka consumers stopped")
    }

    private fun handleSpreadCreatedEvent(event: SpreadCreatedEvent): Mono<Void> {
        logger.info("Processing SpreadCreatedEvent: spreadId=${event.spreadId}")
        // Spread creation doesn't notify anyone specific in this MVP (no follower system)
        return Mono.empty()
    }

    private fun handleInterpretationCreatedEvent(event: InterpretationCreatedEvent): Mono<Void> {
        logger.info("Processing InterpretationCreatedEvent: interpretationId=${event.interpretationId}")

        // Don't notify the spread author if they added their own interpretation
        if (event.spreadAuthorId == event.interpretationAuthorId) {
            logger.debug("Skipping notification - interpretation author is spread author")
            return Mono.empty()
        }

        val notification =
            Notification(
                userId = event.spreadAuthorId,
                type = "NEW_INTERPRETATION",
                title = "New interpretation on your spread",
                message = "${event.interpretationAuthorUsername} added an interpretation: \"${event.textPreview}...\"",
                spreadId = event.spreadId,
                interpretationId = event.interpretationId,
                createdAt = java.time.Instant.now(),
            )

        return notificationRepository
            .save(notification)
            .flatMap { saved ->
                logger.info("Created notification: id=${saved.id} for user=${saved.userId}")
                val dto = notificationMapper.toDto(saved)
                logger.info("Broadcasting notification to WebSocket for user=${saved.userId}")
                notificationBroadcaster.broadcast(saved.userId, dto)
            }.doOnError { error ->
                logger.error("Error during notification save/broadcast", error)
            }
    }
}
