package com.github.butvinmitmo.divinationservice.application.interfaces.provider

import reactor.core.publisher.Mono
import java.util.UUID

interface CurrentUserProvider {
    fun getCurrentUserId(): Mono<UUID>

    fun getCurrentRole(): Mono<String>

    fun canModify(authorId: UUID): Mono<Boolean>
}
