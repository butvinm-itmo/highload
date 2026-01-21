package com.github.butvinmitmo.notificationservice.config

import com.github.butvinmitmo.notificationservice.infrastructure.websocket.NotificationWebSocketHandler
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
    fun webSocketHandlerMapping(): HandlerMapping =
        SimpleUrlHandlerMapping(
            mapOf("/ws/notifications" to notificationWebSocketHandler),
            -1, // Higher priority than RouterFunction handlers
        )

    @Bean
    fun webSocketHandlerAdapter(): WebSocketHandlerAdapter = WebSocketHandlerAdapter()
}
