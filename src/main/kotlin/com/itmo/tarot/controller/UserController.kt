package com.itmo.tarot.controller

import com.itmo.tarot.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management")
@Validated
class UserController(
    private val userService: UserService
) {
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user and all associated data")
    fun deleteUser(
        @Parameter(description = "User ID")
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        userService.deleteUser(id)
        return ResponseEntity.noContent().build()
    }
}