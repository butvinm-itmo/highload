package com.github.butvinmitmo.divinationservice.integration

import com.github.butvinmitmo.divinationservice.repository.InterpretationRepository
import com.github.butvinmitmo.divinationservice.repository.SpreadCardRepository
import com.github.butvinmitmo.divinationservice.repository.SpreadRepository
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
abstract class BaseIntegrationTest {
    @Autowired
    protected lateinit var spreadRepository: SpreadRepository

    @Autowired
    protected lateinit var spreadCardRepository: SpreadCardRepository

    @Autowired
    protected lateinit var interpretationRepository: InterpretationRepository

    @AfterEach
    fun cleanupDatabase() {
        interpretationRepository.deleteAll()
        spreadCardRepository.deleteAll()
        spreadRepository.deleteAll()
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
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
            registry.add("spring.flyway.enabled") { "false" }
            registry.add("services.user-service.url") { wireMock.baseUrl() }
            registry.add("services.tarot-service.url") { wireMock.baseUrl() }
        }
    }
}
