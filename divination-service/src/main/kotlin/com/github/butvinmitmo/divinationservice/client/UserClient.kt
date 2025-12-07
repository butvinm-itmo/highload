package com.github.butvinmitmo.divinationservice.client

import com.github.butvinmitmo.shared.dto.UserDto
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

@FeignClient(name = "user-service", url = "\${services.user-service.url:}")
interface UserClient {
    @GetMapping("/api/internal/users/{id}/entity")
    fun getUserById(
        @PathVariable id: UUID,
    ): UserDto
}
