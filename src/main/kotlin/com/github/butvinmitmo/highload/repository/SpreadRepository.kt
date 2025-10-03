package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.LayoutType
import com.github.butvinmitmo.highload.entity.Spread
import com.github.butvinmitmo.highload.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Repository
interface SpreadRepository : JpaRepository<Spread, UUID> {
    @EntityGraph(attributePaths = ["author", "layoutType"])
    override fun findById(id: UUID): Optional<Spread>

    @EntityGraph(attributePaths = ["author", "layoutType"])
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<Spread>

    @EntityGraph(attributePaths = ["author", "layoutType"])
    fun findByAuthorOrderByCreatedAtDesc(
        author: User,
        pageable: Pageable,
    ): Page<Spread>

    @EntityGraph(attributePaths = ["author", "layoutType"])
    fun findByLayoutTypeOrderByCreatedAtDesc(
        layoutType: LayoutType,
        pageable: Pageable,
    ): Page<Spread>

    @EntityGraph(attributePaths = ["author", "layoutType"])
    @Query("SELECT s FROM Spread s WHERE s.createdAt < :createdAt ORDER BY s.createdAt DESC")
    fun findSpreadsBeforeDate(
        @Param("createdAt") createdAt: Instant,
        pageable: Pageable,
    ): List<Spread>

    @EntityGraph(attributePaths = ["author", "layoutType"])
    @Query(
        """
        SELECT s FROM Spread s
        WHERE s.createdAt < (SELECT s2.createdAt FROM Spread s2 WHERE s2.id = :spreadId)
        OR (s.createdAt = (SELECT s2.createdAt FROM Spread s2 WHERE s2.id = :spreadId) AND s.id < :spreadId)
        ORDER BY s.createdAt DESC, s.id DESC
    """,
    )
    fun findSpreadsAfterCursor(
        @Param("spreadId") spreadId: UUID,
        pageable: Pageable,
    ): List<Spread>

    fun countByAuthor(author: User): Long

    fun deleteAllByAuthor(author: User)

    @EntityGraph(attributePaths = ["author", "layoutType"])
    fun findByQuestionContainingIgnoreCase(
        searchTerm: String,
        pageable: Pageable,
    ): Page<Spread>
}
