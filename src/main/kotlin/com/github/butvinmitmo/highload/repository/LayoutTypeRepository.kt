package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.LayoutType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface LayoutTypeRepository : JpaRepository<LayoutType, UUID> {
    fun findByName(name: String): Optional<LayoutType>

    fun findByCardsCount(cardsCount: Int): List<LayoutType>

    fun existsByName(name: String): Boolean

    fun findAllByOrderByCardsCount(): List<LayoutType>

    @Query(
        """
        SELECT lt.id, lt.name, lt.cardsCount, COUNT(s.id) as usage_count
        FROM LayoutType lt
        LEFT JOIN Spread s ON s.layoutType.id = lt.id
        GROUP BY lt.id, lt.name, lt.cardsCount
        ORDER BY usage_count DESC
    """,
    )
    fun getLayoutTypeStatistics(): List<Array<Any>>

    @Query(
        """
        SELECT lt FROM LayoutType lt
        WHERE lt.id = (
            SELECT s.layoutType.id
            FROM Spread s
            GROUP BY s.layoutType.id
            ORDER BY COUNT(s.id) DESC
            LIMIT 1
        )
    """,
    )
    fun findMostPopularLayoutType(): Optional<LayoutType>

    @Query("SELECT COUNT(s) FROM Spread s WHERE s.layoutType.id = :layoutTypeId")
    fun countSpreadsUsingLayoutType(layoutTypeId: UUID): Long
}
