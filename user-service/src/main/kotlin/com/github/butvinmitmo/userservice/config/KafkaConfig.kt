package com.github.butvinmitmo.userservice.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.butvinmitmo.shared.dto.events.UserEventData
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class KafkaConfig {
    @Bean
    fun producerFactory(
        kafkaProperties: KafkaProperties,
        objectMapper: ObjectMapper,
    ): ProducerFactory<String, UserEventData> {
        val props = kafkaProperties.buildProducerProperties(null)
        val jsonSerializer =
            JsonSerializer<UserEventData>(objectMapper).apply {
                isAddTypeInfo = false
            }
        return DefaultKafkaProducerFactory(
            props,
            StringSerializer(),
            jsonSerializer,
        )
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, UserEventData>): KafkaTemplate<String, UserEventData> =
        KafkaTemplate(producerFactory)
}
