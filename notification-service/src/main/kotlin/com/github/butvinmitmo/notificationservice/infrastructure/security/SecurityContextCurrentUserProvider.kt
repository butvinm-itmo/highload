package com.github.butvinmitmo.notificationservice.infrastructure.security

import com.github.butvinmitmo.notificationservice.application.interfaces.provider.CurrentUserProvider
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
}
