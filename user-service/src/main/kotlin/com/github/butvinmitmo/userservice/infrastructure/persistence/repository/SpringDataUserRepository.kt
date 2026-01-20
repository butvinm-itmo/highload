package com.github.butvinmitmo.userservice.infrastructure.persistence.repository

import com.github.butvinmitmo.userservice.infrastructure.persistence.entity.UserEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface SpringDataUserRepository : R2dbcRepository<UserEntity, UUID> {
    fun findByUsername(username: String): Mono<UserEntity>

    @Query("""SELECT * FROM "user" ORDER BY created_at DESC LIMIT :limit OFFSET :offset""")
    fun findAllPaginated(
        offset: Long,
        limit: Int,
    ): Flux<UserEntity>

    @Query("""SELECT COUNT(*) FROM "user"""")
    override fun count(): Mono<Long>
}
