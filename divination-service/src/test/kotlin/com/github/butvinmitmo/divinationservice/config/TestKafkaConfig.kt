package com.github.butvinmitmo.divinationservice.config

import com.github.butvinmitmo.divinationservice.service.EventPublisher
import com.github.butvinmitmo.shared.dto.InterpretationCreatedEvent
import com.github.butvinmitmo.shared.dto.SpreadCreatedEvent
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender

@TestConfiguration
class TestKafkaConfig {
    private val logger = LoggerFactory.getLogger(TestKafkaConfig::class.java)

    @Bean
    @Primary
    @Suppress("UNCHECKED_CAST")
    fun testKafkaSender(): KafkaSender<String, Any> {
        val mock = Mockito.mock(KafkaSender::class.java) as KafkaSender<String, Any>
        return mock
    }

    @Bean
    @Primary
    fun testEventPublisher(): EventPublisher {
        val mockSender = Mockito.mock(KafkaSender::class.java) as KafkaSender<String, Any>
        return object : EventPublisher(mockSender) {
            override fun publishSpreadCreated(event: SpreadCreatedEvent): Mono<Void> {
                logger.info("TEST: Would publish SpreadCreatedEvent: spreadId=${event.spreadId}")
                return Mono.empty()
            }

            override fun publishInterpretationCreated(event: InterpretationCreatedEvent): Mono<Void> {
                logger.info(
                    "TEST: Would publish InterpretationCreatedEvent: interpretationId=${event.interpretationId}",
                )
                return Mono.empty()
            }
        }
    }
}
