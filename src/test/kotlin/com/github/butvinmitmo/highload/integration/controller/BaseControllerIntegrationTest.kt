package com.github.butvinmitmo.highload.integration.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.butvinmitmo.highload.repository.InterpretationRepository
import com.github.butvinmitmo.highload.repository.LayoutTypeRepository
import com.github.butvinmitmo.highload.repository.SpreadCardRepository
import com.github.butvinmitmo.highload.repository.SpreadRepository
import com.github.butvinmitmo.highload.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

/**
 * Base class for controller integration tests using MockMvc and TestContainers.
 *
 * Tests the full stack: Controller → Service → Repository → Database
 * Uses MockMvc to simulate HTTP requests (not real HTTP).
 *
 * Features:
 * - Shared PostgreSQL container across all controller tests
 * - MockMvc for simulating HTTP requests
 * - Jackson ObjectMapper for JSON serialization
 * - Layout type UUIDs for creating spreads
 * - Automatic database cleanup after each test to prevent data pollution
 *
 * Note: @Transactional is NOT used to avoid rollback interference with multi-step tests.
 * Test data is persisted across requests within the same test method, but cleaned up afterwards.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
abstract class BaseControllerIntegrationTest {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var layoutTypeRepository: LayoutTypeRepository

    @Autowired
    private lateinit var interpretationRepository: InterpretationRepository

    @Autowired
    private lateinit var spreadCardRepository: SpreadCardRepository

    @Autowired
    private lateinit var spreadRepository: SpreadRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    protected lateinit var oneCardLayoutId: UUID
    protected lateinit var threeCardsLayoutId: UUID
    protected lateinit var crossLayoutId: UUID

    @BeforeEach
    fun setupLayoutTypes() {
        oneCardLayoutId =
            layoutTypeRepository.findByName("ONE_CARD")?.id
                ?: throw IllegalStateException("ONE_CARD layout not found")
        threeCardsLayoutId =
            layoutTypeRepository.findByName("THREE_CARDS")?.id
                ?: throw IllegalStateException("THREE_CARDS layout not found")
        crossLayoutId =
            layoutTypeRepository.findByName("CROSS")?.id
                ?: throw IllegalStateException("CROSS layout not found")
    }

    @AfterEach
    fun cleanupDatabase() {
        // Delete in cascade order: interpretations → spread_cards → spreads → users (excluding seed data)
        interpretationRepository.deleteAll()
        spreadCardRepository.deleteAll()
        spreadRepository.deleteAll()

        // Delete all users except the seed admin user
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
