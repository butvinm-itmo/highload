package com.github.butvinmitmo.notificationservice.config

import com.github.butvinmitmo.notificationservice.websocket.NotificationWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@Configuration
class WebSocketConfig(
    private val notificationWebSocketHandler: NotificationWebSocketHandler,
) {
    @Bean
    fun webSocketHandlerMapping(): HandlerMapping {
        val map = mapOf("/api/v0.0.1/notifications/ws" to notificationWebSocketHandler)
        return SimpleUrlHandlerMapping(map, -1)
    }

    @Bean
    fun webSocketHandlerAdapter(): WebSocketHandlerAdapter = WebSocketHandlerAdapter()
}
