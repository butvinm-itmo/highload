package com.github.butvinmitmo.divinationservice.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.butvinmitmo.shared.dto.events.InterpretationEventData
import com.github.butvinmitmo.shared.dto.events.SpreadEventData
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
    fun spreadProducerFactory(
        kafkaProperties: KafkaProperties,
        objectMapper: ObjectMapper,
    ): ProducerFactory<String, SpreadEventData> {
        val props = kafkaProperties.buildProducerProperties(null)
        val jsonSerializer =
            JsonSerializer<SpreadEventData>(objectMapper).apply {
                isAddTypeInfo = false
            }
        return DefaultKafkaProducerFactory(
            props,
            StringSerializer(),
            jsonSerializer,
        )
    }

    @Bean
    fun spreadKafkaTemplate(
        spreadProducerFactory: ProducerFactory<String, SpreadEventData>,
    ): KafkaTemplate<String, SpreadEventData> = KafkaTemplate(spreadProducerFactory)

    @Bean
    fun interpretationProducerFactory(
        kafkaProperties: KafkaProperties,
        objectMapper: ObjectMapper,
    ): ProducerFactory<String, InterpretationEventData> {
        val props = kafkaProperties.buildProducerProperties(null)
        val jsonSerializer =
            JsonSerializer<InterpretationEventData>(objectMapper).apply {
                isAddTypeInfo = false
            }
        return DefaultKafkaProducerFactory(
            props,
            StringSerializer(),
            jsonSerializer,
        )
    }

    @Bean
    fun interpretationKafkaTemplate(
        interpretationProducerFactory: ProducerFactory<String, InterpretationEventData>,
    ): KafkaTemplate<String, InterpretationEventData> = KafkaTemplate(interpretationProducerFactory)
}
