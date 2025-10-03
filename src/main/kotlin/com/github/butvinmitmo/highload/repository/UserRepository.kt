package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface UserRepository {
    fun findById(id: UUID): User?

    fun existsById(id: UUID): Boolean

    fun save(user: User): User

    fun findAll(pageable: Pageable): Page<User>

    fun deleteById(id: UUID)
}
