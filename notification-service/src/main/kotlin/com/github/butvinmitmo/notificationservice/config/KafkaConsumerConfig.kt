package com.github.butvinmitmo.notificationservice.config

import com.github.butvinmitmo.shared.dto.InterpretationCreatedEvent
import com.github.butvinmitmo.shared.dto.SpreadCreatedEvent
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.support.serializer.JsonDeserializer
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.time.Duration

@Configuration
class KafkaConsumerConfig {
    @Value("\${spring.kafka.bootstrap-servers:localhost:9092}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.consumer.group-id:notification-service}")
    private lateinit var groupId: String

    private fun <T> baseReceiverOptions(valueClass: Class<T>): ReceiverOptions<String, T> {
        val props =
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG to groupId,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
                JsonDeserializer.TRUSTED_PACKAGES to "com.github.butvinmitmo.shared.dto",
                JsonDeserializer.VALUE_DEFAULT_TYPE to valueClass.name,
                JsonDeserializer.USE_TYPE_INFO_HEADERS to false,
            )

        return ReceiverOptions
            .create<String, T>(props)
            .commitInterval(Duration.ofSeconds(1))
            .commitBatchSize(100)
    }

    @Bean
    fun spreadEventReceiver(): KafkaReceiver<String, SpreadCreatedEvent> {
        val options =
            baseReceiverOptions(SpreadCreatedEvent::class.java)
                .subscription(listOf("spread-events"))
        return KafkaReceiver.create(options)
    }

    @Bean
    fun interpretationEventReceiver(): KafkaReceiver<String, InterpretationCreatedEvent> {
        val options =
            baseReceiverOptions(InterpretationCreatedEvent::class.java)
                .subscription(listOf("interpretation-events"))
        return KafkaReceiver.create(options)
    }
}
