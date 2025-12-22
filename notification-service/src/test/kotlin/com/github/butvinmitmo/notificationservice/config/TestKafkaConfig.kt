package com.github.butvinmitmo.notificationservice.config

import com.github.butvinmitmo.shared.dto.InterpretationCreatedEvent
import com.github.butvinmitmo.shared.dto.SpreadCreatedEvent
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver

@TestConfiguration
@ConditionalOnProperty(name = ["kafka.mock.enabled"], havingValue = "true", matchIfMissing = true)
class TestKafkaConfig {
    private val logger = LoggerFactory.getLogger(TestKafkaConfig::class.java)

    @Bean
    @Primary
    @Suppress("UNCHECKED_CAST")
    fun testSpreadEventReceiver(): KafkaReceiver<String, SpreadCreatedEvent> {
        logger.info("TEST: Creating mock SpreadEventReceiver that returns Flux.never()")
        val mock = Mockito.mock(KafkaReceiver::class.java) as KafkaReceiver<String, SpreadCreatedEvent>
        whenever(mock.receive()).thenReturn(Flux.never())
        return mock
    }

    @Bean
    @Primary
    @Suppress("UNCHECKED_CAST")
    fun testInterpretationEventReceiver(): KafkaReceiver<String, InterpretationCreatedEvent> {
        logger.info("TEST: Creating mock InterpretationEventReceiver that returns Flux.never()")
        val mock = Mockito.mock(KafkaReceiver::class.java) as KafkaReceiver<String, InterpretationCreatedEvent>
        whenever(mock.receive()).thenReturn(Flux.never())
        return mock
    }
}
