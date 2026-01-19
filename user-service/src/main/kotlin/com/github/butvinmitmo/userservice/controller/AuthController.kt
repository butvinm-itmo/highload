package com.github.butvinmitmo.userservice.controller

import com.github.butvinmitmo.shared.dto.AuthTokenResponse
import com.github.butvinmitmo.shared.dto.LoginRequest
import com.github.butvinmitmo.userservice.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v0.0.1/auth")
@Tag(name = "Authentication", description = "User authentication operations")
@Validated
class AuthController(
    private val userService: UserService,
) {
    @PostMapping("/login")
    @Operation(
        summary = "Authenticate user",
        description = "Authenticates user with username and password, returns JWT token",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Authentication successful, returns JWT token"),
            ApiResponse(responseCode = "401", description = "Invalid credentials"),
            ApiResponse(responseCode = "400", description = "Invalid request data"),
        ],
    )
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): Mono<ResponseEntity<AuthTokenResponse>> = userService.authenticate(request).map { ResponseEntity.ok(it) }
}
