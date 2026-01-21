package com.github.butvinmitmo.userservice.application.interfaces.publisher

import com.github.butvinmitmo.userservice.domain.model.User
import reactor.core.publisher.Mono

interface UserEventPublisher {
    fun publishCreated(user: User): Mono<Void>

    fun publishUpdated(user: User): Mono<Void>

    fun publishDeleted(user: User): Mono<Void>
}
