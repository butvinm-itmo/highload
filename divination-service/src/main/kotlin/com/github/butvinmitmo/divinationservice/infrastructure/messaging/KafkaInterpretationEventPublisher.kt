package com.github.butvinmitmo.divinationservice.infrastructure.messaging

import com.github.butvinmitmo.divinationservice.application.interfaces.publisher.InterpretationEventPublisher
import com.github.butvinmitmo.divinationservice.domain.model.Interpretation
import com.github.butvinmitmo.divinationservice.infrastructure.messaging.mapper.InterpretationEventDataMapper
import com.github.butvinmitmo.shared.dto.events.EventType
import com.github.butvinmitmo.shared.dto.events.InterpretationEventData
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
class KafkaInterpretationEventPublisher(
    @Qualifier("interpretationKafkaTemplate") private val kafkaTemplate: KafkaTemplate<String, InterpretationEventData>,
    private val mapper: InterpretationEventDataMapper,
    @Value("\${kafka.topics.interpretations-events}") private val topic: String,
) : InterpretationEventPublisher {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun publishCreated(interpretation: Interpretation): Mono<Void> = publish(interpretation, EventType.CREATED)

    override fun publishUpdated(interpretation: Interpretation): Mono<Void> = publish(interpretation, EventType.UPDATED)

    override fun publishDeleted(interpretation: Interpretation): Mono<Void> = publish(interpretation, EventType.DELETED)

    private fun publish(
        interpretation: Interpretation,
        eventType: EventType,
    ): Mono<Void> =
        Mono
            .fromCallable {
                val eventData = mapper.toEventData(interpretation)
                val record =
                    ProducerRecord<String, InterpretationEventData>(
                        topic,
                        null,
                        eventData.id.toString(),
                        eventData,
                    ).apply {
                        headers().add("eventType", eventType.name.toByteArray())
                        headers().add("timestamp", Instant.now().toString().toByteArray())
                    }
                kafkaTemplate.send(record).get()
                log.debug("Published {} event for interpretation {}", eventType, interpretation.id)
            }.subscribeOn(Schedulers.boundedElastic())
            .doOnError { e ->
                log.error(
                    "Failed to publish {} event for interpretation {}: {}",
                    eventType,
                    interpretation.id,
                    e.message,
                )
            }.then()
}
