package com.github.butvinmitmo.notificationservice.application.interfaces.provider

import reactor.core.publisher.Mono
import java.util.UUID

interface SpreadProvider {
    fun getSpreadOwnerId(spreadId: UUID): Mono<UUID>
}
