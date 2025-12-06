package com.github.butvinmitmo.e2e.client

import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.CreateUserResponse
import com.github.butvinmitmo.shared.dto.UpdateUserRequest
import com.github.butvinmitmo.shared.dto.UserDto
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@FeignClient(name = "user-service", url = "\${services.user-service.url}")
interface UserServiceClient {
    @PostMapping("/api/v0.0.1/users")
    fun createUser(
        @RequestBody request: CreateUserRequest,
    ): ResponseEntity<CreateUserResponse>

    @GetMapping("/api/v0.0.1/users")
    fun getUsers(
        @RequestParam(defaultValue = "0") page: Int = 0,
        @RequestParam(defaultValue = "50") size: Int = 50,
    ): ResponseEntity<List<UserDto>>

    @GetMapping("/api/v0.0.1/users/{id}")
    fun getUserById(
        @PathVariable id: UUID,
    ): ResponseEntity<UserDto>

    @PutMapping("/api/v0.0.1/users/{id}")
    fun updateUser(
        @PathVariable id: UUID,
        @RequestBody request: UpdateUserRequest,
    ): ResponseEntity<UserDto>

    @DeleteMapping("/api/v0.0.1/users/{id}")
    fun deleteUser(
        @PathVariable id: UUID,
    ): ResponseEntity<Void>

    @GetMapping("/api/internal/users/{id}/entity")
    fun getInternalUser(
        @PathVariable id: UUID,
    ): ResponseEntity<UserDto>
}
