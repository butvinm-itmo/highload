package com.github.butvinmitmo.userservice.controller

import com.github.butvinmitmo.shared.dto.UserDto
import com.github.butvinmitmo.userservice.service.UserService
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/internal/users")
@Hidden
class InternalUserController(
    private val userService: UserService,
) {
    @GetMapping("/{id}/entity")
    fun getUserById(
        @PathVariable id: UUID,
    ): UserDto = userService.getUser(id)
}
