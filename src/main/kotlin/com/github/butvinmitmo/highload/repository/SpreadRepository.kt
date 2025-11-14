package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.Spread
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpreadRepository : JpaRepository<Spread, UUID> {
    @Query(
        """
        SELECT DISTINCT s FROM Spread s
        LEFT JOIN FETCH s.author
        LEFT JOIN FETCH s.layoutType
        LEFT JOIN FETCH s.spreadCards sc
        LEFT JOIN FETCH sc.card
        WHERE s.id = :id
        """,
    )
    fun findByIdWithCards(
        @Param("id") id: UUID,
    ): Spread?

    @Query(
        """
        SELECT DISTINCT s FROM Spread s
        LEFT JOIN FETCH s.author
        LEFT JOIN FETCH s.layoutType
        LEFT JOIN FETCH s.spreadCards sc
        LEFT JOIN FETCH sc.card
        WHERE s.id = :id
        """,
    )
    fun findByIdWithCardsAndInterpretations(
        @Param("id") id: UUID,
    ): Spread?

    @Query("SELECT s FROM Spread s ORDER BY s.createdAt DESC")
    fun findAllOrderByCreatedAtDesc(pageable: Pageable): Page<Spread>

    @Query(
        """
        SELECT s FROM Spread s
        WHERE s.createdAt < (SELECT s2.createdAt FROM Spread s2 WHERE s2.id = :spreadId)
        OR (s.createdAt = (SELECT s2.createdAt FROM Spread s2 WHERE s2.id = :spreadId) AND s.id < :spreadId)
        ORDER BY s.createdAt DESC, s.id DESC
        LIMIT :limit
    """,
    )
    fun findSpreadsAfterCursor(
        @Param("spreadId") spreadId: UUID,
        @Param("limit") limit: Int,
    ): List<Spread>

    @Query("SELECT s FROM Spread s ORDER BY s.createdAt DESC LIMIT :limit")
    fun findLatestSpreads(
        @Param("limit") limit: Int,
    ): List<Spread>
}
