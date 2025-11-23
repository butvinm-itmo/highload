package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.Interpretation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface InterpretationRepository : JpaRepository<Interpretation, UUID> {
    @Query("SELECT COUNT(i) > 0 FROM Interpretation i WHERE i.author.id = :authorId AND i.spread.id = :spreadId")
    fun existsByAuthorAndSpread(
        @Param("authorId") authorId: UUID,
        @Param("spreadId") spreadId: UUID,
    ): Boolean

    @Query(
        """
        SELECT DISTINCT i FROM Interpretation i
        LEFT JOIN FETCH i.author
        WHERE i.spread.id = :spreadId
        ORDER BY i.createdAt ASC
        """,
    )
    fun findBySpreadIdWithAuthor(
        @Param("spreadId") spreadId: UUID,
    ): List<Interpretation>

    @Query(
        """
        SELECT DISTINCT i FROM Interpretation i
        LEFT JOIN FETCH i.author
        WHERE i.spread.id = :spreadId
        ORDER BY i.createdAt DESC
        """,
    )
    fun findBySpreadIdOrderByCreatedAtDesc(
        @Param("spreadId") spreadId: UUID,
    ): List<Interpretation>
}
