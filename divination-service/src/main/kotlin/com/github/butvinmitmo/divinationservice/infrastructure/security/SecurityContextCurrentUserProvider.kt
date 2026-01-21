package com.github.butvinmitmo.divinationservice.infrastructure.security

import com.github.butvinmitmo.divinationservice.application.interfaces.provider.CurrentUserProvider
import com.github.butvinmitmo.shared.security.GatewayAuthenticationToken
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.UUID

@Component
class SecurityContextCurrentUserProvider : CurrentUserProvider {
    override fun getCurrentUserId(): Mono<UUID> =
        ReactiveSecurityContextHolder
            .getContext()
            .map { (it.authentication as GatewayAuthenticationToken).userId }

    override fun getCurrentRole(): Mono<String> =
        ReactiveSecurityContextHolder
            .getContext()
            .map { (it.authentication as GatewayAuthenticationToken).role }

    override fun canModify(authorId: UUID): Mono<Boolean> =
        ReactiveSecurityContextHolder
            .getContext()
            .map { auth ->
                val token = auth.authentication as GatewayAuthenticationToken
                token.userId == authorId || token.role == "ADMIN"
            }
}
