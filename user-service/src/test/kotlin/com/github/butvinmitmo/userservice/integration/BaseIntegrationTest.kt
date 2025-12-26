package com.github.butvinmitmo.userservice.integration

import com.github.butvinmitmo.shared.client.DivinationServiceClient
import com.github.butvinmitmo.userservice.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
abstract class BaseIntegrationTest {
    @Autowired
    protected lateinit var userRepository: UserRepository

    @MockBean
    protected lateinit var divinationServiceClient: DivinationServiceClient

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
        }
    }
}
