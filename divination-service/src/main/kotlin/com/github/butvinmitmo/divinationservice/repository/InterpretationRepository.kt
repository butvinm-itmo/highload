package com.github.butvinmitmo.divinationservice.repository

import com.github.butvinmitmo.divinationservice.entity.Interpretation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface InterpretationRepository : JpaRepository<Interpretation, UUID> {
    @Query("SELECT COUNT(i) > 0 FROM Interpretation i WHERE i.authorId = :authorId AND i.spread.id = :spreadId")
    fun existsByAuthorAndSpread(
        @Param("authorId") authorId: UUID,
        @Param("spreadId") spreadId: UUID,
    ): Boolean

    @Query(
        """
        SELECT i FROM Interpretation i
        WHERE i.spread.id = :spreadId
        ORDER BY i.createdAt DESC
        """,
    )
    fun findBySpreadIdOrderByCreatedAtDesc(
        @Param("spreadId") spreadId: UUID,
        pageable: Pageable,
    ): Page<Interpretation>

    @Query(
        value = """
        SELECT COUNT(i) FROM Interpretation i
        WHERE i.spread.id = :spreadId
        """,
    )
    fun countBySpreadId(
        @Param("spreadId") spreadId: UUID,
    ): Long

    @Query(
        """
        SELECT i.spread.id, COUNT(i) FROM Interpretation i
        WHERE i.spread.id IN :spreadIds
        GROUP BY i.spread.id
        """,
    )
    fun countBySpreadIds(
        @Param("spreadIds") spreadIds: List<UUID>,
    ): List<Array<Any>>
}
