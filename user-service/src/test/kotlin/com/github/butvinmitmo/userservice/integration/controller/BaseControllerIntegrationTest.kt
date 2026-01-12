package com.github.butvinmitmo.userservice.integration.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.butvinmitmo.shared.client.DivinationServiceInternalClient
import com.github.butvinmitmo.userservice.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.ResponseEntity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
abstract class BaseControllerIntegrationTest {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @MockBean
    protected lateinit var divinationServiceInternalClient: DivinationServiceInternalClient

    @BeforeEach
    fun setupMocks() {
        // Default mock behavior: cleanup always succeeds
        whenever(divinationServiceInternalClient.deleteUserData(any())).thenReturn(ResponseEntity.noContent().build())
    }

    @AfterEach
    fun cleanupDatabase() {
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

            // JWT configuration for testing
            registry.add("jwt.secret") { "testSecretKeyThatIsLongEnoughForHS256AlgorithmRequirements!!" }
            registry.add("jwt.expiration-hours") { "24" }
        }
    }
}
