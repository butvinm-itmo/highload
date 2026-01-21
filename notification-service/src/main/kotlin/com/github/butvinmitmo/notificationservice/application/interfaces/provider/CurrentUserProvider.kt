package com.github.butvinmitmo.notificationservice.application.interfaces.provider

import reactor.core.publisher.Mono
import java.util.UUID

interface CurrentUserProvider {
    fun getCurrentUserId(): Mono<UUID>
}
