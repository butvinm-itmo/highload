package com.github.butvinmitmo.tarotservice.repository

import com.github.butvinmitmo.tarotservice.entity.ArcanaType
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface ArcanaTypeRepository : R2dbcRepository<ArcanaType, UUID> {
    fun findByName(name: String): Mono<ArcanaType>
}
