package com.github.butvinmitmo.userservice.repository

import com.github.butvinmitmo.userservice.entity.User
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface UserRepository : R2dbcRepository<User, UUID> {
    fun findByUsername(username: String): Mono<User>

    @Query("""SELECT * FROM "user" ORDER BY created_at DESC LIMIT :limit OFFSET :offset""")
    fun findAllPaginated(
        offset: Long,
        limit: Int,
    ): Flux<User>

    @Query("""SELECT COUNT(*) FROM "user"""")
    override fun count(): Mono<Long>
}
