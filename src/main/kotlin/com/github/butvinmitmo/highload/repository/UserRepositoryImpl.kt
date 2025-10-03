package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class UserRepositoryImpl(
    private val jpaRepository: UserRepositoryJpa
) : UserRepository {

    override fun findById(id: UUID): User? {
        return jpaRepository.findById(id).orElse(null)
    }

    override fun existsById(id: UUID): Boolean {
        return jpaRepository.existsById(id)
    }

    override fun save(user: User): User {
        return jpaRepository.save(user)
    }

    override fun findAll(pageable: Pageable): Page<User> {
        return jpaRepository.findAll(pageable)
    }

    override fun deleteById(id: UUID) {
        jpaRepository.deleteById(id)
    }
}