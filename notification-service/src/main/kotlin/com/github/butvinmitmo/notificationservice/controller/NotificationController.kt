package com.github.butvinmitmo.notificationservice.controller

import com.github.butvinmitmo.notificationservice.service.NotificationService
import com.github.butvinmitmo.shared.dto.MarkAllReadResponse
import com.github.butvinmitmo.shared.dto.NotificationDto
import com.github.butvinmitmo.shared.dto.UnreadCountResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/v0.0.1/notifications")
@Tag(name = "Notifications", description = "In-app notification management")
@Validated
class NotificationController(
    private val notificationService: NotificationService,
) {
    @GetMapping
    @Operation(
        summary = "Get user notifications",
        description = "Retrieves paginated notifications for the authenticated user",
    )
    fun getNotifications(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) size: Int,
    ): Mono<ResponseEntity<List<NotificationDto>>> =
        notificationService
            .getNotifications(userId, page, size)
            .map { response ->
                ResponseEntity
                    .ok()
                    .header("X-Total-Count", response.totalElements.toString())
                    .body(response.content)
            }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    fun getUnreadCount(
        @RequestHeader("X-User-Id") userId: UUID,
    ): Mono<ResponseEntity<UnreadCountResponse>> =
        notificationService
            .getUnreadCount(userId)
            .map { ResponseEntity.ok(it) }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    fun markAsRead(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): Mono<ResponseEntity<NotificationDto>> =
        notificationService
            .markAsRead(id, userId)
            .map { ResponseEntity.ok(it) }

    @PostMapping("/mark-all-read")
    @Operation(summary = "Mark all notifications as read")
    fun markAllAsRead(
        @RequestHeader("X-User-Id") userId: UUID,
    ): Mono<ResponseEntity<MarkAllReadResponse>> =
        notificationService
            .markAllAsRead(userId)
            .map { ResponseEntity.ok(it) }
}
