package com.github.butvinmitmo.divinationservice.service

import com.github.butvinmitmo.shared.dto.InterpretationCreatedEvent
import com.github.butvinmitmo.shared.dto.SpreadCreatedEvent
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderRecord

@Service
open class EventPublisher(
    private val kafkaSender: KafkaSender<String, Any>,
) {
    private val logger = LoggerFactory.getLogger(EventPublisher::class.java)

    companion object {
        const val SPREAD_EVENTS_TOPIC = "spread-events"
        const val INTERPRETATION_EVENTS_TOPIC = "interpretation-events"
    }

    open fun publishSpreadCreated(event: SpreadCreatedEvent): Mono<Void> {
        val record = ProducerRecord(SPREAD_EVENTS_TOPIC, event.spreadId.toString(), event as Any)
        val senderRecord = SenderRecord.create(record, event.eventId)

        return kafkaSender
            .send(Mono.just(senderRecord))
            .doOnNext { result ->
                logger.info(
                    "Published SpreadCreatedEvent: spreadId=${event.spreadId}, " +
                        "offset=${result.recordMetadata().offset()}",
                )
            }.doOnError { error ->
                logger.error("Failed to publish SpreadCreatedEvent: spreadId=${event.spreadId}", error)
            }.then()
    }

    open fun publishInterpretationCreated(event: InterpretationCreatedEvent): Mono<Void> {
        val record = ProducerRecord(INTERPRETATION_EVENTS_TOPIC, event.spreadId.toString(), event as Any)
        val senderRecord = SenderRecord.create(record, event.eventId)

        return kafkaSender
            .send(Mono.just(senderRecord))
            .doOnNext { result ->
                logger.info(
                    "Published InterpretationCreatedEvent: interpretationId=${event.interpretationId}, " +
                        "offset=${result.recordMetadata().offset()}",
                )
            }.doOnError { error ->
                logger.error(
                    "Failed to publish InterpretationCreatedEvent: interpretationId=${event.interpretationId}",
                    error,
                )
            }.then()
    }
}
