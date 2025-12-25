package com.github.butvinmitmo.notificationservice.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.butvinmitmo.shared.dto.NotificationDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketMessage
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Component
class NotificationBroadcaster(
    private val sessionRegistry: WebSocketSessionRegistry,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(NotificationBroadcaster::class.java)

    fun broadcast(
        userId: UUID,
        notification: NotificationDto,
    ): Mono<Void> {
        val sessions = sessionRegistry.getSessions(userId)
        if (sessions.isEmpty()) {
            logger.info(
                "No active WebSocket sessions for user $userId (active users: ${sessionRegistry.getActiveUserCount()})",
            )
            return Mono.empty()
        }

        val json = objectMapper.writeValueAsString(notification)
        logger.info("Broadcasting notification to ${sessions.size} session(s) for user $userId")

        return Flux
            .fromIterable(sessions)
            .flatMap { session ->
                val message: WebSocketMessage = session.textMessage(json)
                session
                    .send(Mono.just(message))
                    .doOnError { error ->
                        logger.error("Failed to send notification to session ${session.id}", error)
                    }.onErrorResume { Mono.empty() }
            }.then()
    }
}
