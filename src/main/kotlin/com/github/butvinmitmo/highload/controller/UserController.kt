package com.github.butvinmitmo.highload.controller

import com.github.butvinmitmo.highload.dto.CreateUserRequest
import com.github.butvinmitmo.highload.dto.UpdateUserRequest
import com.github.butvinmitmo.highload.dto.UserDto
import com.github.butvinmitmo.highload.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v0.0.1/users")
@Tag(name = "Users", description = "User management operations")
class UserController(
    private val userService: UserService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a new user",
        description =
            "Creates a new user with the specified username. " +
                "ID and timestamp are auto-generated. Returns 409 if username already exists.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "User created successfully"),
            ApiResponse(responseCode = "409", description = "User with this username already exists"),
            ApiResponse(responseCode = "400", description = "Invalid request data"),
        ],
    )
    fun createUser(
        @Valid @RequestBody request: CreateUserRequest,
    ): UserDto = userService.createUser(request)

    @GetMapping
    @Operation(
        summary = "Get paginated list of users",
        description = "Retrieves a paginated list of all users in the system",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        ],
    )
    fun getUsers(
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0")
        page: Int,
        @Parameter(description = "Page size", example = "20")
        @RequestParam(defaultValue = "20")
        size: Int,
    ): List<UserDto> = userService.getUsers(page, size)

    @GetMapping("/{id}")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieves a specific user by their UUID",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "User found"),
            ApiResponse(responseCode = "404", description = "User not found"),
        ],
    )
    fun getUser(
        @Parameter(description = "User ID", required = true)
        @PathVariable
        id: UUID,
    ): UserDto = userService.getUser(id)

    @PutMapping("/{id}")
    @Operation(
        summary = "Update user",
        description = "Updates an existing user's information",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "User updated successfully"),
            ApiResponse(responseCode = "404", description = "User not found"),
            ApiResponse(responseCode = "400", description = "Invalid request data"),
        ],
    )
    fun updateUser(
        @Parameter(description = "User ID", required = true)
        @PathVariable
        id: UUID,
        @Valid @RequestBody request: UpdateUserRequest,
    ): UserDto = userService.updateUser(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Delete user",
        description = "Deletes a user and all their associated data (spreads, interpretations) in a single transaction",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "User deleted successfully"),
            ApiResponse(responseCode = "404", description = "User not found"),
        ],
    )
    fun deleteUser(
        @Parameter(description = "User ID", required = true)
        @PathVariable
        id: UUID,
    ) {
        userService.deleteUser(id)
    }
}
