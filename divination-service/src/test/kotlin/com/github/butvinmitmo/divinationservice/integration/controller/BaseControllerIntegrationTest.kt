package com.github.butvinmitmo.divinationservice.integration.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.butvinmitmo.divinationservice.application.interfaces.publisher.InterpretationEventPublisher
import com.github.butvinmitmo.divinationservice.application.interfaces.publisher.SpreadEventPublisher
import com.github.butvinmitmo.divinationservice.config.TestFeignConfiguration
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.repository.SpringDataInterpretationRepository
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.repository.SpringDataSpreadCardRepository
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.repository.SpringDataSpreadRepository
import com.github.butvinmitmo.shared.client.TarotServiceClient
import com.github.butvinmitmo.shared.client.UserServiceClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
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
    protected lateinit var spreadRepository: SpringDataSpreadRepository

    @Autowired
    protected lateinit var spreadCardRepository: SpringDataSpreadCardRepository

    @Autowired
    protected lateinit var interpretationRepository: SpringDataInterpretationRepository

    @org.springframework.boot.test.mock.mockito.MockBean
    protected lateinit var userServiceClient: UserServiceClient

    @org.springframework.boot.test.mock.mockito.MockBean
    protected lateinit var tarotServiceClient: TarotServiceClient

    @org.springframework.boot.test.mock.mockito.MockBean
    protected lateinit var spreadEventPublisher: SpreadEventPublisher

    @org.springframework.boot.test.mock.mockito.MockBean
    protected lateinit var interpretationEventPublisher: InterpretationEventPublisher

    protected val testUserId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    protected val oneCardLayoutId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000020")
    protected val threeCardsLayoutId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000021")
    protected val crossLayoutId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000022")

    // System context used by providers for internal Feign calls
    protected val systemUserId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    protected val systemRole: String = "SYSTEM"

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(userServiceClient, tarotServiceClient)
        whenever(spreadEventPublisher.publishCreated(any())).thenReturn(Mono.empty())
        whenever(spreadEventPublisher.publishDeleted(any())).thenReturn(Mono.empty())
        whenever(interpretationEventPublisher.publishCreated(any())).thenReturn(Mono.empty())
        whenever(interpretationEventPublisher.publishUpdated(any())).thenReturn(Mono.empty())
        whenever(interpretationEventPublisher.publishDeleted(any())).thenReturn(Mono.empty())
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
