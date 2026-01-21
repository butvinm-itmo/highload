package com.github.butvinmitmo.divinationservice.integration

import com.github.butvinmitmo.divinationservice.application.interfaces.publisher.InterpretationEventPublisher
import com.github.butvinmitmo.divinationservice.application.interfaces.publisher.SpreadEventPublisher
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.repository.SpringDataInterpretationRepository
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.repository.SpringDataSpreadCardRepository
import com.github.butvinmitmo.divinationservice.infrastructure.persistence.repository.SpringDataSpreadRepository
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import reactor.core.publisher.Mono

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
abstract class BaseIntegrationTest {
    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @Autowired
    protected lateinit var spreadRepository: SpringDataSpreadRepository

    @Autowired
    protected lateinit var spreadCardRepository: SpringDataSpreadCardRepository

    @Autowired
    protected lateinit var interpretationRepository: SpringDataInterpretationRepository

    @MockBean
    protected lateinit var spreadEventPublisher: SpreadEventPublisher

    @MockBean
    protected lateinit var interpretationEventPublisher: InterpretationEventPublisher

    @BeforeEach
    fun setupPublisherMocks() {
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
        @RegisterExtension
        val wireMock: WireMockExtension =
            WireMockExtension
                .newInstance()
                .options(
                    com.github.tomakehurst.wiremock.core.WireMockConfiguration
                        .wireMockConfig()
                        .dynamicPort(),
                ).build()

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
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("services.user-service.url") { wireMock.baseUrl() }
            registry.add("services.tarot-service.url") { wireMock.baseUrl() }
        }
    }
}
