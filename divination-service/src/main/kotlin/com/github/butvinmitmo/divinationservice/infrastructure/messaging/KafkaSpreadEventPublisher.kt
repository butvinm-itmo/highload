package com.github.butvinmitmo.divinationservice.infrastructure.messaging

import com.github.butvinmitmo.divinationservice.application.interfaces.publisher.SpreadEventPublisher
import com.github.butvinmitmo.divinationservice.domain.model.Spread
import com.github.butvinmitmo.divinationservice.infrastructure.messaging.mapper.SpreadEventDataMapper
import com.github.butvinmitmo.shared.dto.events.EventType
import com.github.butvinmitmo.shared.dto.events.SpreadEventData
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Instant

@Component
class KafkaSpreadEventPublisher(
    @Qualifier("spreadKafkaTemplate") private val kafkaTemplate: KafkaTemplate<String, SpreadEventData>,
    private val mapper: SpreadEventDataMapper,
    @Value("\${kafka.topics.spreads-events}") private val topic: String,
) : SpreadEventPublisher {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun publishCreated(spread: Spread): Mono<Void> = publish(spread, EventType.CREATED)

    override fun publishDeleted(spread: Spread): Mono<Void> = publish(spread, EventType.DELETED)

    private fun publish(
        spread: Spread,
        eventType: EventType,
    ): Mono<Void> =
        Mono
            .fromCallable {
                val eventData = mapper.toEventData(spread)
                val record =
                    ProducerRecord<String, SpreadEventData>(topic, null, eventData.id.toString(), eventData).apply {
                        headers().add("eventType", eventType.name.toByteArray())
                        headers().add("timestamp", Instant.now().toString().toByteArray())
                    }
                kafkaTemplate.send(record).get()
                log.debug("Published {} event for spread {}", eventType, spread.id)
            }.subscribeOn(Schedulers.boundedElastic())
            .doOnError { e ->
                log.error("Failed to publish {} event for spread {}: {}", eventType, spread.id, e.message)
            }.then()
}
