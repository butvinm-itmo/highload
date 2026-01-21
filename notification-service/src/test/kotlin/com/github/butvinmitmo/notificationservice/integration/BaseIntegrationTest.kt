package com.github.butvinmitmo.notificationservice.integration

import com.github.butvinmitmo.notificationservice.application.interfaces.provider.SpreadProvider
import com.github.butvinmitmo.notificationservice.infrastructure.persistence.repository.SpringDataNotificationRepository
import com.github.butvinmitmo.notificationservice.infrastructure.websocket.WebSocketSessionManager
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
abstract class BaseIntegrationTest {
    @Autowired
    protected lateinit var springDataNotificationRepository: SpringDataNotificationRepository

    @MockBean
    protected lateinit var spreadProvider: SpreadProvider

    @MockBean
    protected lateinit var webSocketSessionManager: WebSocketSessionManager

    @AfterEach
    fun cleanupDatabase() {
        springDataNotificationRepository.deleteAll().block()
    }

    companion object {
        @JvmStatic
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer("postgres:15-alpine")
                .withDatabaseName("tarot_db_test")
                .withUsername("test_user")
                .withPassword("test_password")
                .apply {
                    start()
                }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
            registry.add("spring.flyway.url") { postgres.jdbcUrl }
            registry.add("spring.flyway.user") { postgres.username }
            registry.add("spring.flyway.password") { postgres.password }
            registry.add("spring.flyway.enabled") { "true" }
            // Disable Kafka for tests
            registry.add("spring.kafka.bootstrap-servers") { "localhost:9092" }
            registry.add("spring.kafka.listener.auto-startup") { "false" }
        }
    }
}
