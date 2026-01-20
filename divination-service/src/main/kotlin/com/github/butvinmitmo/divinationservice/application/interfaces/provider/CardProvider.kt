package com.github.butvinmitmo.divinationservice.application.interfaces.provider

import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.LayoutTypeDto
import reactor.core.publisher.Mono
import java.util.UUID

interface CardProvider {
    fun getLayoutTypeById(
        requesterId: UUID,
        requesterRole: String,
        layoutTypeId: UUID,
    ): Mono<LayoutTypeDto>

    fun getRandomCards(
        requesterId: UUID,
        requesterRole: String,
        count: Int,
    ): Mono<List<CardDto>>

    fun getAllCards(): Mono<List<CardDto>>

    fun getSystemLayoutType(layoutTypeId: UUID): Mono<LayoutTypeDto>
}
