package com.github.butvinmitmo.e2e

import com.github.butvinmitmo.shared.client.DivinationServiceClient
import com.github.butvinmitmo.shared.client.TarotServiceClient
import com.github.butvinmitmo.shared.client.UserServiceClient
import feign.FeignException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.time.Duration

/**
 * Base class for E2E tests.
 *
 * Uses TestContainers to automatically start all services via docker compose.
 */
@SpringBootTest(classes = [E2ETestApplication::class])
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseE2ETest {
    @Autowired
    protected lateinit var userClient: UserServiceClient

    @Autowired
    protected lateinit var tarotClient: TarotServiceClient

    @Autowired
    protected lateinit var divinationClient: DivinationServiceClient

    companion object {
        private const val CONFIG_SERVER = "config-server"
        private const val EUREKA_SERVER = "eureka-server"
        private const val GATEWAY_SERVICE = "gateway-service"
        private const val POSTGRES = "postgres"
        private const val USER_SERVICE = "user-service"
        private const val TAROT_SERVICE = "tarot-service"
        private const val DIVINATION_SERVICE = "divination-service"

        private const val CONFIG_SERVER_PORT = 8888
        private const val EUREKA_SERVER_PORT = 8761
        private const val GATEWAY_PORT = 8080
        private const val POSTGRES_PORT = 5432
        private const val USER_SERVICE_PORT = 8081
        private const val TAROT_SERVICE_PORT = 8082
        private const val DIVINATION_SERVICE_PORT = 8083

        private val STARTUP_TIMEOUT: Duration = Duration.ofMinutes(5)

        @JvmStatic
        val compose: ComposeContainer =
            ComposeContainer(File("docker-compose.yml"))
                .withLocalCompose(true)
                .withExposedService(
                    CONFIG_SERVER,
                    CONFIG_SERVER_PORT,
                    Wait
                        .forHttp("/actuator/health")
                        .forStatusCode(200)
                        .withStartupTimeout(STARTUP_TIMEOUT),
                ).withExposedService(
                    EUREKA_SERVER,
                    EUREKA_SERVER_PORT,
                    Wait
                        .forHttp("/actuator/health")
                        .forStatusCode(200)
                        .withStartupTimeout(STARTUP_TIMEOUT),
                ).withExposedService(
                    GATEWAY_SERVICE,
                    GATEWAY_PORT,
                    Wait
                        .forHttp("/actuator/health")
                        .forStatusCode(200)
                        .withStartupTimeout(STARTUP_TIMEOUT),
                ).withExposedService(
                    POSTGRES,
                    POSTGRES_PORT,
                    Wait
                        .forListeningPort()
                        .withStartupTimeout(STARTUP_TIMEOUT),
                ).withExposedService(
                    USER_SERVICE,
                    USER_SERVICE_PORT,
                    Wait
                        .forHttp("/actuator/health")
                        .forStatusCode(200)
                        .withStartupTimeout(STARTUP_TIMEOUT),
                ).withExposedService(
                    TAROT_SERVICE,
                    TAROT_SERVICE_PORT,
                    Wait
                        .forHttp("/actuator/health")
                        .forStatusCode(200)
                        .withStartupTimeout(STARTUP_TIMEOUT),
                ).withExposedService(
                    DIVINATION_SERVICE,
                    DIVINATION_SERVICE_PORT,
                    Wait
                        .forHttp("/actuator/health")
                        .forStatusCode(200)
                        .withStartupTimeout(STARTUP_TIMEOUT),
                ).apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            val gatewayHost = compose.getServiceHost(GATEWAY_SERVICE, GATEWAY_PORT)
            val gatewayPort = compose.getServicePort(GATEWAY_SERVICE, GATEWAY_PORT)

            registry.add("services.user-service.url") { "http://$gatewayHost:$gatewayPort" }
            registry.add("services.tarot-service.url") { "http://$gatewayHost:$gatewayPort" }
            registry.add("services.divination-service.url") { "http://$gatewayHost:$gatewayPort" }
        }
    }

    protected fun assertThrowsWithStatus(
        expectedStatus: Int,
        block: () -> Any,
    ): FeignException {
        val exception = assertThrows<FeignException> { block() }
        assertEquals(
            expectedStatus,
            exception.status(),
            "Expected HTTP status $expectedStatus but got ${exception.status()}",
        )
        return exception
    }
}
