package com.github.butvinmitmo.notificationservice.websocket

import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketSession
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class WebSocketSessionRegistry {
    private val sessions = ConcurrentHashMap<UUID, MutableSet<WebSocketSession>>()

    fun register(
        userId: UUID,
        session: WebSocketSession,
    ) {
        sessions.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(session)
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
        }
    }

    fun getSessions(userId: UUID): Set<WebSocketSession> = sessions[userId]?.toSet() ?: emptySet()

    fun getActiveUserCount(): Int = sessions.size

    fun getActiveSessionCount(): Int = sessions.values.sumOf { it.size }
}
