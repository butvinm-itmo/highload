package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.Interpretation
import com.github.butvinmitmo.highload.entity.Spread
import com.github.butvinmitmo.highload.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface InterpretationRepository : JpaRepository<Interpretation, UUID> {
    @EntityGraph(attributePaths = ["author", "spread"])
    override fun findById(id: UUID): Optional<Interpretation>

    @EntityGraph(attributePaths = ["author"])
    fun findBySpreadOrderByCreatedAtDesc(spread: Spread): List<Interpretation>

    @EntityGraph(attributePaths = ["author"])
    fun findBySpreadOrderByCreatedAtDesc(
        spread: Spread,
        pageable: Pageable,
    ): Page<Interpretation>

    @EntityGraph(attributePaths = ["spread", "author"])
    fun findByAuthorOrderByCreatedAtDesc(
        author: User,
        pageable: Pageable,
    ): Page<Interpretation>

    @EntityGraph(attributePaths = ["author", "spread"])
    fun findByAuthorAndSpread(
        author: User,
        spread: Spread,
    ): Optional<Interpretation>

    fun existsByAuthorAndSpread(
        author: User,
        spread: Spread,
    ): Boolean

    fun countBySpread(spread: Spread): Long

    fun countByAuthor(author: User): Long

    @Modifying
    @Query("DELETE FROM Interpretation i WHERE i.spread = :spread")
    fun deleteAllBySpread(
        @Param("spread") spread: Spread,
    )

    @Modifying
    @Query("DELETE FROM Interpretation i WHERE i.author = :author")
    fun deleteAllByAuthor(
        @Param("author") author: User,
    )

    @EntityGraph(attributePaths = ["author", "spread"])
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<Interpretation>

    fun findByIdAndAuthor(
        id: UUID,
        author: User,
    ): Optional<Interpretation>
}
