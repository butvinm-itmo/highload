package com.github.butvinmitmo.notificationservice.integration.kafka

import com.github.butvinmitmo.notificationservice.repository.NotificationRepository
import com.github.butvinmitmo.shared.dto.InterpretationCreatedEvent
import com.github.butvinmitmo.shared.dto.SpreadCreatedEvent
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("kafka-test")
@Testcontainers
class EventConsumerIntegrationTest {
    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @AfterEach
    fun cleanupDatabase() {
        notificationRepository.deleteAll().block()
    }

    companion object {
        // Test user IDs from init-test-db.sql
        val TEST_USER_1_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val TEST_USER_2_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")

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
            // Flyway JDBC configuration
            registry.add("spring.flyway.url", postgres::getJdbcUrl)
            registry.add("spring.flyway.user", postgres::getUsername)
            registry.add("spring.flyway.password", postgres::getPassword)
            registry.add("spring.flyway.enabled") { "true" }

            // R2DBC configuration
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username", postgres::getUsername)
            registry.add("spring.r2dbc.password", postgres::getPassword)

            // Kafka configuration
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
        }

        private fun createKafkaSender(): KafkaSender<String, Any> {
            val props =
                mapOf(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
                    ProducerConfig.ACKS_CONFIG to "all",
                    JsonSerializer.ADD_TYPE_INFO_HEADERS to false,
                )
            val senderOptions = SenderOptions.create<String, Any>(props)
            return KafkaSender.create(senderOptions)
        }
    }

    @Test
    fun `should create notification when InterpretationCreatedEvent is consumed`() {
        // Wait for consumer to be ready
        Thread.sleep(5000)

        // Use test users from init-test-db.sql (user 1 is spread author, user 2 is interpretation author)
        val spreadAuthorId = TEST_USER_1_ID
        val interpretationAuthorId = TEST_USER_2_ID
        val spreadId = UUID.randomUUID()
        val interpretationId = UUID.randomUUID()

        val event =
            InterpretationCreatedEvent(
                interpretationId = interpretationId,
                spreadId = spreadId,
                spreadAuthorId = spreadAuthorId,
                interpretationAuthorId = interpretationAuthorId,
                interpretationAuthorUsername = "testMedium",
                textPreview = "This spread reveals great fortune",
            )

        val sender = createKafkaSender()
        val record = ProducerRecord("interpretation-events", spreadId.toString(), event as Any)
        val senderRecord = SenderRecord.create(record, event.eventId)

        sender
            .send(
                reactor.core.publisher.Mono
                    .just(senderRecord),
            ).blockLast()
        sender.close()

        await.atMost(Duration.ofSeconds(10)).untilAsserted {
            val notifications = notificationRepository.findAll().collectList().block()!!
            assertEquals(1, notifications.size)

            val notification = notifications.first()
            assertEquals(spreadAuthorId, notification.userId)
            assertEquals("NEW_INTERPRETATION", notification.type)
            assertEquals("New interpretation on your spread", notification.title)
            assertTrue(notification.message.contains("testMedium"))
            assertTrue(notification.message.contains("This spread reveals great fortune"))
            assertEquals(spreadId, notification.spreadId)
            assertEquals(interpretationId, notification.interpretationId)
            assertEquals(false, notification.isRead)
        }
    }

    @Test
    fun `should NOT create notification when interpretation author is spread author`() {
        // Use same test user for both spread and interpretation author
        val authorId = TEST_USER_1_ID
        val spreadId = UUID.randomUUID()
        val interpretationId = UUID.randomUUID()

        val event =
            InterpretationCreatedEvent(
                interpretationId = interpretationId,
                spreadId = spreadId,
                spreadAuthorId = authorId,
                interpretationAuthorId = authorId,
                interpretationAuthorUsername = "selfInterpreter",
                textPreview = "My own interpretation",
            )

        val sender = createKafkaSender()
        val record = ProducerRecord("interpretation-events", spreadId.toString(), event as Any)
        val senderRecord = SenderRecord.create(record, event.eventId)

        sender
            .send(
                reactor.core.publisher.Mono
                    .just(senderRecord),
            ).blockLast()
        sender.close()

        Thread.sleep(3000)

        val notifications = notificationRepository.findAll().collectList().block()!!
        assertEquals(0, notifications.size)
    }

    @Test
    fun `should handle SpreadCreatedEvent without creating notification`() {
        val authorId = TEST_USER_1_ID
        val spreadId = UUID.randomUUID()

        val event =
            SpreadCreatedEvent(
                spreadId = spreadId,
                authorId = authorId,
                authorUsername = "testUser",
                question = "What does the future hold?",
                layoutTypeName = "THREE_CARDS",
                cardsCount = 3,
            )

        val sender = createKafkaSender()
        val record = ProducerRecord("spread-events", spreadId.toString(), event as Any)
        val senderRecord = SenderRecord.create(record, event.eventId)

        sender
            .send(
                reactor.core.publisher.Mono
                    .just(senderRecord),
            ).blockLast()
        sender.close()

        Thread.sleep(3000)

        val notifications = notificationRepository.findAll().collectList().block()!!
        assertEquals(0, notifications.size)
    }

    @Test
    fun `should create multiple notifications for multiple events`() {
        // Wait for consumer to be ready
        Thread.sleep(5000)

        // User 1 is the spread author, User 2 is the interpretation author
        val spreadAuthorId = TEST_USER_1_ID
        val interpretationAuthorId = TEST_USER_2_ID

        val sender = createKafkaSender()

        repeat(3) { i ->
            val event =
                InterpretationCreatedEvent(
                    interpretationId = UUID.randomUUID(),
                    spreadId = UUID.randomUUID(),
                    spreadAuthorId = spreadAuthorId,
                    interpretationAuthorId = interpretationAuthorId,
                    interpretationAuthorUsername = "medium$i",
                    textPreview = "Interpretation $i",
                )

            val record = ProducerRecord("interpretation-events", event.spreadId.toString(), event as Any)
            val senderRecord = SenderRecord.create(record, event.eventId)
            sender
                .send(
                    reactor.core.publisher.Mono
                        .just(senderRecord),
                ).blockLast()
        }

        sender.close()

        await.atMost(Duration.ofSeconds(15)).untilAsserted {
            val notifications = notificationRepository.findAll().collectList().block()!!
            assertEquals(3, notifications.size)
            assertTrue(notifications.all { it.userId == spreadAuthorId })
        }
    }
}
