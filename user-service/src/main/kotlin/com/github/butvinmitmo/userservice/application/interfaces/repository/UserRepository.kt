package com.github.butvinmitmo.userservice.application.interfaces.repository

import com.github.butvinmitmo.userservice.domain.model.User
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface UserRepository {
    fun findById(id: UUID): Mono<User>

    fun findByUsername(username: String): Mono<User>

    fun findAllPaginated(
        offset: Long,
        limit: Int,
    ): Flux<User>

    fun count(): Mono<Long>

    fun save(user: User): Mono<User>

    fun existsById(id: UUID): Mono<Boolean>

    fun deleteById(id: UUID): Mono<Void>
}
