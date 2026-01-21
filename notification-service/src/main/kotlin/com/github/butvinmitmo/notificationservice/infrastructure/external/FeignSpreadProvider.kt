package com.github.butvinmitmo.notificationservice.infrastructure.external

import com.github.butvinmitmo.notificationservice.application.interfaces.provider.SpreadProvider
import com.github.butvinmitmo.shared.client.DivinationServiceInternalClient
import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.UUID

@Component
class FeignSpreadProvider(
    private val divinationServiceInternalClient: DivinationServiceInternalClient,
) : SpreadProvider {
    private val logger = LoggerFactory.getLogger(FeignSpreadProvider::class.java)

    override fun getSpreadOwnerId(spreadId: UUID): Mono<UUID> =
        Mono
            .fromCallable {
                divinationServiceInternalClient.getSpreadOwner(spreadId).body
            }.subscribeOn(Schedulers.boundedElastic())
            .flatMap { ownerId ->
                if (ownerId != null) Mono.just(ownerId) else Mono.empty()
            }.onErrorResume { e ->
                when {
                    e is FeignException.NotFound -> {
                        logger.debug("Spread {} not found, skipping notification", spreadId)
                        Mono.empty()
                    }
                    else -> {
                        logger.error("Error fetching spread owner for spread {}: {}", spreadId, e.message)
                        Mono.error(e)
                    }
                }
            }
}
