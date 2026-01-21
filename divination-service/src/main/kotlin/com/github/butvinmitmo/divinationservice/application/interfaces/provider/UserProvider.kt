package com.github.butvinmitmo.divinationservice.application.interfaces.provider

import com.github.butvinmitmo.shared.dto.UserDto
import reactor.core.publisher.Mono
import java.util.UUID

interface UserProvider {
    fun getUserById(
        requesterId: UUID,
        requesterRole: String,
        userId: UUID,
    ): Mono<UserDto>

    fun getSystemUser(userId: UUID): Mono<UserDto>
}
