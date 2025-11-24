package com.github.butvinmitmo.highload.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.butvinmitmo.highload.repository.LayoutTypeRepository
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
 * Base class for E2E API tests using MockMvc and TestContainers.
 *
 * Features:
 * - Shared PostgreSQL container across all test classes
 * - MockMvc for simulating HTTP requests
 * - Jackson ObjectMapper for JSON serialization
 * - Layout type UUIDs for creating spreads
 *
 * Note: @Transactional is NOT used to avoid rollback interference with multi-step tests.
 * Test data is persisted across requests within the same test method.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
abstract class BaseE2ETest {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var layoutTypeRepository: LayoutTypeRepository

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
