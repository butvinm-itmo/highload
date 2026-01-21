package com.github.butvinmitmo.filesservice.application.interfaces.provider

import reactor.core.publisher.Mono
import java.util.UUID

interface CurrentUserProvider {
    fun getCurrentUserId(): Mono<UUID>
}
