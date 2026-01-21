package com.github.butvinmitmo.divinationservice.application.interfaces.publisher

import com.github.butvinmitmo.divinationservice.domain.model.Spread
import reactor.core.publisher.Mono

interface SpreadEventPublisher {
    fun publishCreated(spread: Spread): Mono<Void>

    fun publishDeleted(spread: Spread): Mono<Void>
}
