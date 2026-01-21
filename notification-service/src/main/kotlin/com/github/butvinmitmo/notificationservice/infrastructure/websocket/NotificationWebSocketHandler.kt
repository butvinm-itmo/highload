package com.github.butvinmitmo.notificationservice.infrastructure.websocket

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.util.UUID

@Component
class NotificationWebSocketHandler(
    private val sessionManager: WebSocketSessionManager,
) : WebSocketHandler {
    private val logger = LoggerFactory.getLogger(NotificationWebSocketHandler::class.java)

    override fun handle(session: WebSocketSession): Mono<Void> {
        val userId = extractUserId(session)
        if (userId == null) {
            logger.warn("WebSocket connection rejected: no X-User-Id header in session {}", session.id)
            return session.close()
        }

        sessionManager.register(userId, session)

        // Keep connection alive by receiving and ignoring incoming messages
        // Session cleanup happens when the receive stream completes (disconnect)
        return session
            .receive()
            .doOnNext { message ->
                // Ignore incoming messages - this is a push-only channel
                logger.debug("Received message from user {}, ignoring", userId)
            }.doFinally {
                sessionManager.unregister(userId, session)
                logger.info("WebSocket connection closed for user {}", userId)
            }.then()
    }

    private fun extractUserId(session: WebSocketSession): UUID? {
        // Gateway adds X-User-Id header after JWT validation
        val userIdHeader = session.handshakeInfo.headers.getFirst("X-User-Id")
        return userIdHeader?.let {
            try {
                UUID.fromString(it)
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid X-User-Id header: {}", it)
                null
            }
        }
    }
}
