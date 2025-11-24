package com.github.butvinmitmo.highload.integration

import com.github.butvinmitmo.highload.repository.InterpretationRepository
import com.github.butvinmitmo.highload.repository.SpreadCardRepository
import com.github.butvinmitmo.highload.repository.SpreadRepository
import com.github.butvinmitmo.highload.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

/**
 * Base class for integration tests using TestContainers.
 *
 * Features:
 * - Shared PostgreSQL container across all integration test classes
 * - Automatic Spring Boot context configuration
 * - Database connection properties configured via DynamicPropertySource
 * - Automatic database cleanup after each test to prevent data pollution
 *
 * All integration tests should extend this class to share the same container instance,
 * improving test execution speed.
 */
@SpringBootTest
@Testcontainers
abstract class BaseIntegrationTest {
    @Autowired
    private lateinit var interpretationRepository: InterpretationRepository

    @Autowired
    private lateinit var spreadCardRepository: SpreadCardRepository

    @Autowired
    private lateinit var spreadRepository: SpreadRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @AfterEach
    fun cleanupDatabase() {
        // Delete in cascade order: interpretations → spread_cards → spreads → users (excluding seed data)
        interpretationRepository.deleteAll()
        spreadCardRepository.deleteAll()
        spreadRepository.deleteAll()

        // Delete all users except the seed admin user (UUID-based filtering)
        val seedUserId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        userRepository
            .findAll()
            .filter { it.id != seedUserId }
            .forEach { userRepository.delete(it) }
    }

    companion object {
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
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
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
        }
    }
}
