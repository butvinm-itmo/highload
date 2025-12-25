package com.github.butvinmitmo.divinationservice.integration.kafka

import com.github.butvinmitmo.divinationservice.service.EventPublisher
import com.github.butvinmitmo.shared.dto.InterpretationCreatedEvent
import com.github.butvinmitmo.shared.dto.SpreadCreatedEvent
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("kafka-test")
@Testcontainers
class EventPublisherIntegrationTest {
    @Autowired
    private lateinit var eventPublisher: EventPublisher

    companion object {
        @JvmStatic
        val kafka: KafkaContainer =
            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
                .withKraft()
                .apply {
                    start()
                    createTopics()
                }

        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:15-alpine")
                .withDatabaseName("tarot_db_test")
                .withUsername("test_user")
                .withPassword("test_password")
                .withInitScript("init-test-db.sql")
                .apply { start() }

        private fun KafkaContainer.createTopics() {
            val adminProps = mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers)
            AdminClient.create(adminProps).use { admin ->
                val topics =
                    listOf(
                        NewTopic("spread-events", 1, 1.toShort()),
                        NewTopic("interpretation-events", 1, 1.toShort()),
                    )
                admin.createTopics(topics).all().get()
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // Database configuration
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username", postgres::getUsername)
            registry.add("spring.r2dbc.password", postgres::getPassword)
            registry.add("spring.flyway.url", postgres::getJdbcUrl)
            registry.add("spring.flyway.user", postgres::getUsername)
            registry.add("spring.flyway.password", postgres::getPassword)
            registry.add("spring.flyway.enabled") { "true" }

            // Kafka configuration
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
        }

        private fun <T> createKafkaReceiver(
            topic: String,
            valueClass: Class<T>,
        ): KafkaReceiver<String, T> {
            val props =
                mapOf(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                    ConsumerConfig.GROUP_ID_CONFIG to "test-consumer-${System.currentTimeMillis()}",
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                    JsonDeserializer.TRUSTED_PACKAGES to "com.github.butvinmitmo.shared.dto",
                    JsonDeserializer.VALUE_DEFAULT_TYPE to valueClass.name,
                    JsonDeserializer.USE_TYPE_INFO_HEADERS to false,
                )
            val options =
                ReceiverOptions
                    .create<String, T>(props)
                    .subscription(listOf(topic))
            return KafkaReceiver.create(options)
        }
    }

    @Test
    fun `should publish SpreadCreatedEvent to Kafka`() {
        val spreadId = UUID.randomUUID()
        val authorId = UUID.randomUUID()

        val event =
            SpreadCreatedEvent(
                spreadId = spreadId,
                authorId = authorId,
                authorUsername = "testUser",
                question = "What does the future hold?",
                layoutTypeName = "THREE_CARDS",
                cardsCount = 3,
            )

        // Create a consumer first (to avoid missing the event)
        val receiver = createKafkaReceiver("spread-events", SpreadCreatedEvent::class.java)
        val receivedEvents = mutableListOf<SpreadCreatedEvent>()

        val subscription =
            receiver
                .receive()
                .filter { it.value().spreadId == spreadId }
                .take(1)
                .subscribe { record ->
                    receivedEvents.add(record.value())
                    record.receiverOffset().acknowledge()
                }

        // Wait a bit for consumer to be ready, then publish
        Thread.sleep(2000)
        eventPublisher.publishSpreadCreated(event).block()

        try {
            await.atMost(Duration.ofSeconds(10)).untilAsserted {
                assertEquals(1, receivedEvents.size)

                val receivedEvent = receivedEvents.first()
                assertEquals(spreadId, receivedEvent.spreadId)
                assertEquals(authorId, receivedEvent.authorId)
                assertEquals("testUser", receivedEvent.authorUsername)
                assertEquals("What does the future hold?", receivedEvent.question)
                assertEquals("THREE_CARDS", receivedEvent.layoutTypeName)
                assertEquals(3, receivedEvent.cardsCount)
                assertNotNull(receivedEvent.eventId)
                assertNotNull(receivedEvent.timestamp)
            }
        } finally {
            subscription.dispose()
        }
    }

    @Test
    fun `should publish InterpretationCreatedEvent to Kafka`() {
        val interpretationId = UUID.randomUUID()
        val spreadId = UUID.randomUUID()
        val spreadAuthorId = UUID.randomUUID()
        val interpretationAuthorId = UUID.randomUUID()

        val event =
            InterpretationCreatedEvent(
                interpretationId = interpretationId,
                spreadId = spreadId,
                spreadAuthorId = spreadAuthorId,
                interpretationAuthorId = interpretationAuthorId,
                interpretationAuthorUsername = "testMedium",
                textPreview = "This spread reveals great fortune",
            )

        // Create a consumer first (to avoid missing the event)
        val receiver = createKafkaReceiver("interpretation-events", InterpretationCreatedEvent::class.java)
        val receivedEvents = mutableListOf<InterpretationCreatedEvent>()

        val subscription =
            receiver
                .receive()
                .filter { it.value().interpretationId == interpretationId }
                .take(1)
                .subscribe { record ->
                    receivedEvents.add(record.value())
                    record.receiverOffset().acknowledge()
                }

        // Wait a bit for consumer to be ready, then publish
        Thread.sleep(2000)
        eventPublisher.publishInterpretationCreated(event).block()

        try {
            await.atMost(Duration.ofSeconds(10)).untilAsserted {
                assertEquals(1, receivedEvents.size)

                val receivedEvent = receivedEvents.first()
                assertEquals(interpretationId, receivedEvent.interpretationId)
                assertEquals(spreadId, receivedEvent.spreadId)
                assertEquals(spreadAuthorId, receivedEvent.spreadAuthorId)
                assertEquals(interpretationAuthorId, receivedEvent.interpretationAuthorId)
                assertEquals("testMedium", receivedEvent.interpretationAuthorUsername)
                assertEquals("This spread reveals great fortune", receivedEvent.textPreview)
                assertNotNull(receivedEvent.eventId)
                assertNotNull(receivedEvent.timestamp)
            }
        } finally {
            subscription.dispose()
        }
    }

    @Test
    fun `should publish multiple events in order`() {
        val receiver = createKafkaReceiver("spread-events", SpreadCreatedEvent::class.java)
        val receivedEvents = mutableListOf<SpreadCreatedEvent>()

        val subscription =
            receiver
                .receive()
                .take(3)
                .subscribe { record ->
                    receivedEvents.add(record.value())
                    record.receiverOffset().acknowledge()
                }

        try {
            // Publish 3 events
            repeat(3) { i ->
                val event =
                    SpreadCreatedEvent(
                        spreadId = UUID.randomUUID(),
                        authorId = UUID.randomUUID(),
                        authorUsername = "user$i",
                        question = "Question $i",
                        layoutTypeName = "ONE_CARD",
                        cardsCount = 1,
                    )
                eventPublisher.publishSpreadCreated(event).block()
            }

            await.atMost(Duration.ofSeconds(15)).untilAsserted {
                assertEquals(3, receivedEvents.size)
                assertTrue(receivedEvents.map { it.authorUsername }.containsAll(listOf("user0", "user1", "user2")))
            }
        } finally {
            subscription.dispose()
        }
    }
}
