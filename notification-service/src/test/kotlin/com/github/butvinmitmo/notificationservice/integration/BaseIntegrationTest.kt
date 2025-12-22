package com.github.butvinmitmo.notificationservice.integration

import com.github.butvinmitmo.notificationservice.config.TestKafkaConfig
import com.github.butvinmitmo.notificationservice.repository.NotificationRepository
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Import(TestKafkaConfig::class)
abstract class BaseIntegrationTest {
    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @Autowired
    protected lateinit var notificationRepository: NotificationRepository

    @AfterEach
    fun cleanupDatabase() {
        notificationRepository.deleteAll().block()
    }

    companion object {
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:15-alpine")
                .withDatabaseName("tarot_db_test")
                .withUsername("test_user")
                .withPassword("test_password")
                .withInitScript("init-test-db.sql")
                .apply {
                    start()
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
        }
    }
}
