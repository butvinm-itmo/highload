package com.github.butvinmitmo.notificationservice.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.butvinmitmo.shared.dto.events.InterpretationEventData
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer

@Configuration
class KafkaConsumerConfig {
    @Bean
    fun interpretationConsumerFactory(
        kafkaProperties: KafkaProperties,
        objectMapper: ObjectMapper,
    ): ConsumerFactory<String, InterpretationEventData> {
        val props = kafkaProperties.buildConsumerProperties(null)
        val jsonDeserializer =
            JsonDeserializer(InterpretationEventData::class.java, objectMapper).apply {
                setRemoveTypeHeaders(false)
                addTrustedPackages("*")
                setUseTypeMapperForKey(false)
            }
        return DefaultKafkaConsumerFactory(
            props,
            StringDeserializer(),
            jsonDeserializer,
        )
    }

    @Bean
    fun interpretationKafkaListenerContainerFactory(
        interpretationConsumerFactory: ConsumerFactory<String, InterpretationEventData>,
    ): ConcurrentKafkaListenerContainerFactory<String, InterpretationEventData> =
        ConcurrentKafkaListenerContainerFactory<String, InterpretationEventData>().apply {
            consumerFactory = interpretationConsumerFactory
        }
}
