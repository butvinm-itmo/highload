package com.github.butvinmitmo.divinationservice.security

import com.github.butvinmitmo.shared.security.GatewayAuthenticationToken
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class AuthorizationService {
    fun getCurrentUserId(): Mono<UUID> =
        ReactiveSecurityContextHolder
            .getContext()
            .map { (it.authentication as GatewayAuthenticationToken).userId }

    fun getCurrentRole(): Mono<String> =
        ReactiveSecurityContextHolder
            .getContext()
            .map { (it.authentication as GatewayAuthenticationToken).role }

    fun canModify(authorId: UUID): Mono<Boolean> =
        ReactiveSecurityContextHolder
            .getContext()
            .map { auth ->
                val token = auth.authentication as GatewayAuthenticationToken
                token.userId == authorId || token.role == "ADMIN"
            }
}
