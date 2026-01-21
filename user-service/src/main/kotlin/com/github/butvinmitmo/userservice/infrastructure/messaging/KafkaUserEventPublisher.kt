package com.github.butvinmitmo.userservice.infrastructure.messaging

import com.github.butvinmitmo.shared.dto.events.EventType
import com.github.butvinmitmo.shared.dto.events.UserEventData
import com.github.butvinmitmo.userservice.application.interfaces.publisher.UserEventPublisher
import com.github.butvinmitmo.userservice.domain.model.User
import com.github.butvinmitmo.userservice.infrastructure.messaging.mapper.UserEventDataMapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Instant

@Component
class KafkaUserEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, UserEventData>,
    private val mapper: UserEventDataMapper,
    @Value("\${kafka.topics.users-events}") private val topic: String,
) : UserEventPublisher {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun publishCreated(user: User): Mono<Void> = publish(user, EventType.CREATED)

    override fun publishUpdated(user: User): Mono<Void> = publish(user, EventType.UPDATED)

    override fun publishDeleted(user: User): Mono<Void> = publish(user, EventType.DELETED)

    private fun publish(
        user: User,
        eventType: EventType,
    ): Mono<Void> =
        Mono
            .fromCallable {
                val eventData = mapper.toEventData(user)
                val record =
                    ProducerRecord<String, UserEventData>(topic, null, eventData.id.toString(), eventData).apply {
                        headers().add("eventType", eventType.name.toByteArray())
                        headers().add("timestamp", Instant.now().toString().toByteArray())
                    }
                kafkaTemplate.send(record).get()
                log.debug("Published {} event for user {}", eventType, user.id)
            }.subscribeOn(Schedulers.boundedElastic())
            .doOnError { e -> log.error("Failed to publish {} event for user {}: {}", eventType, user.id, e.message) }
            .then()
}
