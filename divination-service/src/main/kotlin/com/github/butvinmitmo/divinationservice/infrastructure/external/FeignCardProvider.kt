package com.github.butvinmitmo.divinationservice.infrastructure.external

import com.github.butvinmitmo.divinationservice.application.interfaces.provider.CardProvider
import com.github.butvinmitmo.shared.client.TarotServiceClient
import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.LayoutTypeDto
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.UUID

@Component
class FeignCardProvider(
    private val tarotServiceClient: TarotServiceClient,
) : CardProvider {
    private val systemUserId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private val systemRole = "SYSTEM"

    override fun getLayoutTypeById(
        requesterId: UUID,
        requesterRole: String,
        layoutTypeId: UUID,
    ): Mono<LayoutTypeDto> =
        Mono
            .fromCallable { tarotServiceClient.getLayoutTypeById(requesterId, requesterRole, layoutTypeId).body!! }
            .subscribeOn(Schedulers.boundedElastic())

    override fun getRandomCards(
        requesterId: UUID,
        requesterRole: String,
        count: Int,
    ): Mono<List<CardDto>> =
        Mono
            .fromCallable { tarotServiceClient.getRandomCards(requesterId, requesterRole, count).body!! }
            .subscribeOn(Schedulers.boundedElastic())

    override fun getAllCards(): Mono<List<CardDto>> =
        Mono
            .fromCallable {
                val allCards = mutableListOf<CardDto>()
                val pageSize = 50
                var page = 0
                var fetched: List<CardDto>
                do {
                    fetched = tarotServiceClient.getCards(systemUserId, systemRole, page, pageSize).body!!
                    allCards.addAll(fetched)
                    page++
                } while (fetched.size == pageSize)
                allCards.toList()
            }.subscribeOn(Schedulers.boundedElastic())

    override fun getSystemLayoutType(layoutTypeId: UUID): Mono<LayoutTypeDto> =
        Mono
            .fromCallable { tarotServiceClient.getLayoutTypeById(systemUserId, systemRole, layoutTypeId).body!! }
            .subscribeOn(Schedulers.boundedElastic())
}
