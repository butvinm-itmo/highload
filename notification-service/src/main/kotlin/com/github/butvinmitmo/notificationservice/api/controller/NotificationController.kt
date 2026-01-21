package com.github.butvinmitmo.notificationservice.api.controller

import com.github.butvinmitmo.notificationservice.api.mapper.NotificationDtoMapper
import com.github.butvinmitmo.notificationservice.application.interfaces.provider.CurrentUserProvider
import com.github.butvinmitmo.notificationservice.application.service.NotificationService
import com.github.butvinmitmo.shared.dto.NotificationDto
import com.github.butvinmitmo.shared.dto.UnreadCountResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/v0.0.1/notifications")
@Tag(name = "Notifications", description = "User notification operations")
@Validated
class NotificationController(
    private val notificationService: NotificationService,
    private val currentUserProvider: CurrentUserProvider,
    private val notificationDtoMapper: NotificationDtoMapper,
) {
    @GetMapping
    @Operation(
        summary = "Get paginated list of notifications",
        description = "Retrieves notifications for the current user. Can filter by read status.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Notifications retrieved successfully",
                headers = [
                    Header(
                        name = "X-Total-Count",
                        description = "Total number of notifications",
                        schema = Schema(type = "integer"),
                    ),
                ],
                content = [Content(array = ArraySchema(schema = Schema(implementation = NotificationDto::class)))],
            ),
            ApiResponse(responseCode = "401", description = "Not authenticated"),
        ],
    )
    fun getNotifications(
        @Parameter(description = "Filter by read status (optional)")
        @RequestParam(required = false)
        isRead: Boolean?,
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0")
        @Min(0)
        page: Int,
        @Parameter(description = "Page size (max 50)", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(50)
        size: Int,
    ): Mono<ResponseEntity<List<NotificationDto>>> =
        currentUserProvider.getCurrentUserId().flatMap { userId ->
            notificationService.getNotificationsForUser(userId, isRead, page, size).map { response ->
                ResponseEntity
                    .ok()
                    .header("X-Total-Count", response.totalElements.toString())
                    .body(response.content.map { notificationDtoMapper.toDto(it) })
            }
        }

    @GetMapping("/unread-count")
    @Operation(
        summary = "Get unread notification count",
        description = "Returns the number of unread notifications for the current user.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Unread count retrieved successfully",
                content = [Content(schema = Schema(implementation = UnreadCountResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Not authenticated"),
        ],
    )
    fun getUnreadCount(): Mono<ResponseEntity<UnreadCountResponse>> =
        currentUserProvider.getCurrentUserId().flatMap { userId ->
            notificationService.getUnreadCountForUser(userId).map { count ->
                ResponseEntity.ok(UnreadCountResponse(count = count))
            }
        }

    @PutMapping("/{id}/read")
    @Operation(
        summary = "Mark notification as read",
        description = "Marks a specific notification as read for the current user.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Notification marked as read",
                content = [Content(schema = Schema(implementation = NotificationDto::class))],
            ),
            ApiResponse(responseCode = "404", description = "Notification not found"),
            ApiResponse(responseCode = "401", description = "Not authenticated"),
        ],
    )
    fun markAsRead(
        @Parameter(description = "Notification ID", required = true)
        @PathVariable
        id: UUID,
    ): Mono<ResponseEntity<NotificationDto>> =
        currentUserProvider.getCurrentUserId().flatMap { userId ->
            notificationService.markAsRead(id, userId).map { notification ->
                ResponseEntity.ok(notificationDtoMapper.toDto(notification))
            }
        }
}
