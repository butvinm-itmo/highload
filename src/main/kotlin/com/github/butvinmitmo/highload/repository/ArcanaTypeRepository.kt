package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.ArcanaType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ArcanaTypeRepository : JpaRepository<ArcanaType, UUID> {
    fun findByName(name: String): Optional<ArcanaType>

    fun existsByName(name: String): Boolean

    fun findAllByOrderByName(): List<ArcanaType>

    @Query(
        """
        SELECT at.id, at.name, COUNT(c.id) as card_count
        FROM ArcanaType at
        LEFT JOIN Card c ON c.arcanaType.id = at.id
        GROUP BY at.id, at.name
        ORDER BY card_count DESC
    """,
    )
    fun getArcanaTypeStatistics(): List<Array<Any>>

    @Query("SELECT COUNT(c) FROM Card c WHERE c.arcanaType.id = :arcanaTypeId")
    fun countCardsByArcanaType(arcanaTypeId: UUID): Long

    @Query(
        value = """
        SELECT at.name, COUNT(sc.id) as usage_count
        FROM arcana_type at
        JOIN card c ON c.arcana_type_id = at.id
        JOIN spread_card sc ON sc.card_id = c.id
        GROUP BY at.id, at.name
        ORDER BY usage_count DESC
    """,
        nativeQuery = true,
    )
    fun getArcanaTypeDistributionInSpreads(): List<Array<Any>>
}
