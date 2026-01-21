package com.github.butvinmitmo.notificationservice.infrastructure.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.butvinmitmo.notificationservice.domain.model.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Component
class WebSocketSessionManager(
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(WebSocketSessionManager::class.java)
    private val sessions = ConcurrentHashMap<UUID, CopyOnWriteArrayList<WebSocketSession>>()

    fun register(
        userId: UUID,
        session: WebSocketSession,
    ) {
        sessions.computeIfAbsent(userId) { CopyOnWriteArrayList() }.add(session)
        logger.info("Registered WebSocket session {} for user {}", session.id, userId)
    }

    fun unregister(
        userId: UUID,
        session: WebSocketSession,
    ) {
        sessions[userId]?.let { userSessions ->
            userSessions.remove(session)
            if (userSessions.isEmpty()) {
                sessions.remove(userId)
            }
            logger.info("Unregistered WebSocket session {} for user {}", session.id, userId)
        }
    }

    fun sendToUser(
        userId: UUID,
        notification: Notification,
    ): Mono<Void> {
        val userSessions = sessions[userId] ?: return Mono.empty()

        if (userSessions.isEmpty()) {
            return Mono.empty()
        }

        val json = objectMapper.writeValueAsString(NotificationMessage.from(notification))

        return Mono
            .fromCallable {
                userSessions.forEach { session ->
                    try {
                        if (session.isOpen) {
                            session.send(Mono.just(session.textMessage(json))).subscribe()
                        }
                    } catch (e: Exception) {
                        logger.warn("Failed to send to session {}: {}", session.id, e.message)
                    }
                }
            }.then()
    }

    data class NotificationMessage(
        val id: UUID,
        val spreadId: UUID,
        val interpretationAuthorId: UUID,
        val title: String,
        val message: String,
        val createdAt: String,
    ) {
        companion object {
            fun from(notification: Notification): NotificationMessage =
                NotificationMessage(
                    id = notification.id!!,
                    spreadId = notification.spreadId,
                    interpretationAuthorId = notification.interpretationAuthorId,
                    title = notification.title,
                    message = notification.message,
                    createdAt = notification.createdAt.toString(),
                )
        }
    }
}
