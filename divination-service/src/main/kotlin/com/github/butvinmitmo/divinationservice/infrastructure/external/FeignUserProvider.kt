package com.github.butvinmitmo.divinationservice.infrastructure.external

import com.github.butvinmitmo.divinationservice.application.interfaces.provider.UserProvider
import com.github.butvinmitmo.shared.client.UserServiceClient
import com.github.butvinmitmo.shared.dto.UserDto
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.UUID

@Component
class FeignUserProvider(
    private val userServiceClient: UserServiceClient,
) : UserProvider {
    private val systemUserId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private val systemRole = "SYSTEM"

    override fun getUserById(
        requesterId: UUID,
        requesterRole: String,
        userId: UUID,
    ): Mono<UserDto> =
        Mono
            .fromCallable { userServiceClient.getUserById(requesterId, requesterRole, userId).body!! }
            .subscribeOn(Schedulers.boundedElastic())

    override fun getSystemUser(userId: UUID): Mono<UserDto> =
        Mono
            .fromCallable { userServiceClient.getUserById(systemUserId, systemRole, userId).body!! }
            .subscribeOn(Schedulers.boundedElastic())
}
