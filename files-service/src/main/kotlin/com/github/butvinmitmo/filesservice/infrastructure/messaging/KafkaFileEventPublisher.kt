package com.github.butvinmitmo.filesservice.infrastructure.messaging

import com.github.butvinmitmo.filesservice.application.interfaces.publisher.FileEventPublisher
import com.github.butvinmitmo.filesservice.domain.model.FileUpload
import com.github.butvinmitmo.filesservice.infrastructure.messaging.mapper.FileEventDataMapper
import com.github.butvinmitmo.shared.dto.events.EventType
import com.github.butvinmitmo.shared.dto.events.FileEventData
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Instant

@Component
class KafkaFileEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, FileEventData>,
    private val mapper: FileEventDataMapper,
    @Value("\${kafka.topics.files-events}") private val topic: String,
) : FileEventPublisher {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun publishCompleted(fileUpload: FileUpload): Mono<Void> = publish(fileUpload, EventType.CREATED)

    override fun publishDeleted(fileUpload: FileUpload): Mono<Void> = publish(fileUpload, EventType.DELETED)

    private fun publish(
        fileUpload: FileUpload,
        eventType: EventType,
    ): Mono<Void> =
        Mono
            .fromCallable {
                val eventData = mapper.toEventData(fileUpload)
                val record =
                    ProducerRecord<String, FileEventData>(topic, null, eventData.uploadId.toString(), eventData).apply {
                        headers().add("eventType", eventType.name.toByteArray())
                        headers().add("timestamp", Instant.now().toString().toByteArray())
                    }
                kafkaTemplate.send(record).get()
                log.debug("Published {} event for file upload {}", eventType, fileUpload.id)
            }.subscribeOn(Schedulers.boundedElastic())
            .doOnError { e ->
                log.error("Failed to publish {} event for file upload {}: {}", eventType, fileUpload.id, e.message)
            }.then()
}
