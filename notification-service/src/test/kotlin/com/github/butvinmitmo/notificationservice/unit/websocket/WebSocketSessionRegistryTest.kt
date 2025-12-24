package com.github.butvinmitmo.notificationservice.unit.websocket

import com.github.butvinmitmo.notificationservice.websocket.WebSocketSessionRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.reactive.socket.WebSocketSession
import java.util.UUID

class WebSocketSessionRegistryTest {
    private lateinit var registry: WebSocketSessionRegistry

    @BeforeEach
    fun setUp() {
        registry = WebSocketSessionRegistry()
    }

    @Test
    fun `register should add session for user`() {
        val userId = UUID.randomUUID()
        val session = mockSession("session-1")

        registry.register(userId, session)

        val sessions = registry.getSessions(userId)
        assertEquals(1, sessions.size)
        assertTrue(sessions.contains(session))
    }

    @Test
    fun `register should support multiple sessions per user`() {
        val userId = UUID.randomUUID()
        val session1 = mockSession("session-1")
        val session2 = mockSession("session-2")

        registry.register(userId, session1)
        registry.register(userId, session2)

        val sessions = registry.getSessions(userId)
        assertEquals(2, sessions.size)
        assertTrue(sessions.contains(session1))
        assertTrue(sessions.contains(session2))
    }

    @Test
    fun `unregister should remove session for user`() {
        val userId = UUID.randomUUID()
        val session = mockSession("session-1")

        registry.register(userId, session)
        registry.unregister(userId, session)

        val sessions = registry.getSessions(userId)
        assertTrue(sessions.isEmpty())
    }

    @Test
    fun `unregister should keep other sessions when one is removed`() {
        val userId = UUID.randomUUID()
        val session1 = mockSession("session-1")
        val session2 = mockSession("session-2")

        registry.register(userId, session1)
        registry.register(userId, session2)
        registry.unregister(userId, session1)

        val sessions = registry.getSessions(userId)
        assertEquals(1, sessions.size)
        assertTrue(sessions.contains(session2))
    }

    @Test
    fun `getSessions should return empty set for unknown user`() {
        val unknownUserId = UUID.randomUUID()

        val sessions = registry.getSessions(unknownUserId)

        assertTrue(sessions.isEmpty())
    }

    @Test
    fun `getActiveUserCount should return correct count`() {
        val userId1 = UUID.randomUUID()
        val userId2 = UUID.randomUUID()

        registry.register(userId1, mockSession("session-1"))
        registry.register(userId2, mockSession("session-2"))

        assertEquals(2, registry.getActiveUserCount())
    }

    @Test
    fun `getActiveSessionCount should return total session count`() {
        val userId1 = UUID.randomUUID()
        val userId2 = UUID.randomUUID()

        registry.register(userId1, mockSession("session-1"))
        registry.register(userId1, mockSession("session-2"))
        registry.register(userId2, mockSession("session-3"))

        assertEquals(3, registry.getActiveSessionCount())
    }

    private fun mockSession(id: String): WebSocketSession {
        val session = mock<WebSocketSession>()
        whenever(session.id).thenReturn(id)
        return session
    }
}
