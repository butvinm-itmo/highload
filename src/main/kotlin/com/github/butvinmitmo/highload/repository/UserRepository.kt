package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByUsername(username: String): Optional<User>

    fun existsByUsername(username: String): Boolean

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<User>

    fun findByUsernameContainingIgnoreCase(
        searchTerm: String,
        pageable: Pageable,
    ): Page<User>

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt > (SELECT u2.createdAt FROM User u2 WHERE u2.id = :userId)")
    fun countUsersCreatedAfter(userId: UUID): Long
}
