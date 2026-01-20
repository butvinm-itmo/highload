package com.github.butvinmitmo.divinationservice.application.interfaces.publisher

import com.github.butvinmitmo.divinationservice.domain.model.Interpretation
import reactor.core.publisher.Mono

interface InterpretationEventPublisher {
    fun publishCreated(interpretation: Interpretation): Mono<Void>

    fun publishUpdated(interpretation: Interpretation): Mono<Void>

    fun publishDeleted(interpretation: Interpretation): Mono<Void>
}
