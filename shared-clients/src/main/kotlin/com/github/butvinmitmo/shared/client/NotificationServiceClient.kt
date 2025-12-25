package com.github.butvinmitmo.shared.client

import com.github.butvinmitmo.shared.dto.MarkAllReadResponse
import com.github.butvinmitmo.shared.dto.NotificationDto
import com.github.butvinmitmo.shared.dto.UnreadCountResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@FeignClient(name = "notification-service", url = "\${services.notification-service.url:}")
interface NotificationServiceClient {
    @GetMapping("/api/v0.0.1/notifications")
    fun getNotifications(
        @RequestParam(defaultValue = "0") page: Int = 0,
        @RequestParam(defaultValue = "20") size: Int = 20,
    ): ResponseEntity<List<NotificationDto>>

    @GetMapping("/api/v0.0.1/notifications/unread-count")
    fun getUnreadCount(): ResponseEntity<UnreadCountResponse>

    @PatchMapping("/api/v0.0.1/notifications/{id}/read")
    fun markAsRead(
        @PathVariable id: UUID,
    ): ResponseEntity<NotificationDto>

    @PostMapping("/api/v0.0.1/notifications/mark-all-read")
    fun markAllAsRead(): ResponseEntity<MarkAllReadResponse>
}
