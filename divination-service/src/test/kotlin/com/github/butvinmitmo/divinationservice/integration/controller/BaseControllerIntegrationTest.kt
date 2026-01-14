package com.github.butvinmitmo.divinationservice.integration.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.butvinmitmo.divinationservice.config.TestFeignConfiguration
import com.github.butvinmitmo.divinationservice.repository.InterpretationRepository
import com.github.butvinmitmo.divinationservice.repository.SpreadCardRepository
import com.github.butvinmitmo.divinationservice.repository.SpreadRepository
import com.github.butvinmitmo.shared.client.TarotServiceClient
import com.github.butvinmitmo.shared.client.UserServiceClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import reactor.core.publisher.Mono
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Import(TestFeignConfiguration::class)
abstract class BaseControllerIntegrationTest {
    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var spreadRepository: SpreadRepository

    @Autowired
    protected lateinit var spreadCardRepository: SpreadCardRepository

    @Autowired
    protected lateinit var interpretationRepository: InterpretationRepository

    @org.springframework.boot.test.mock.mockito.MockBean
    protected lateinit var userServiceClient: UserServiceClient

    @org.springframework.boot.test.mock.mockito.MockBean
    protected lateinit var tarotServiceClient: TarotServiceClient

    protected val testUserId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    protected val oneCardLayoutId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000020")
    protected val threeCardsLayoutId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000021")
    protected val crossLayoutId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000022")

    // System context used by mappers for internal Feign calls
    protected val systemUserId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    protected val systemRole: String = "SYSTEM"

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(userServiceClient, tarotServiceClient)
    }

    @AfterEach
    fun cleanupDatabase() {
        Mono
            .`when`(
                interpretationRepository.deleteAll(),
                spreadCardRepository.deleteAll(),
                spreadRepository.deleteAll(),
            ).block()
    }

    companion object {
        @JvmStatic
        val postgres: PostgreSQLContainer =
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
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("spring.flyway.url") { postgres.jdbcUrl }
            registry.add("spring.flyway.user") { postgres.username }
            registry.add("spring.flyway.password") { postgres.password }
        }
    }
}
