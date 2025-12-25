package com.github.butvinmitmo.notificationservice.websocket

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.util.UUID

@Component
class NotificationWebSocketHandler(
    private val sessionRegistry: WebSocketSessionRegistry,
) : WebSocketHandler {
    private val logger = LoggerFactory.getLogger(NotificationWebSocketHandler::class.java)

    override fun handle(session: WebSocketSession): Mono<Void> {
        val userId = extractUserId(session) ?: return session.close()

        logger.info("WebSocket connection opened: userId=$userId, sessionId=${session.id}")
        sessionRegistry.register(userId, session)

        return session
            .receive()
            .doOnTerminate {
                logger.info("WebSocket connection closed: userId=$userId, sessionId=${session.id}")
                sessionRegistry.unregister(userId, session)
            }.doOnError { error ->
                logger.error("WebSocket error: userId=$userId, sessionId=${session.id}", error)
            }.then()
    }

    private fun extractUserId(session: WebSocketSession): UUID? {
        val userIdHeader = session.handshakeInfo.headers.getFirst("X-User-Id")
        return userIdHeader?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        }
    }
}
