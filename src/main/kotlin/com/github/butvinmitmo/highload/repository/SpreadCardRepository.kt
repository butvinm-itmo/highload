package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.Card
import com.github.butvinmitmo.highload.entity.Spread
import com.github.butvinmitmo.highload.entity.SpreadCard
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpreadCardRepository : JpaRepository<SpreadCard, UUID> {
    @EntityGraph(attributePaths = ["card", "card.arcanaType", "spread"])
    fun findBySpreadOrderByPositionInSpread(spread: Spread): List<SpreadCard>

    @EntityGraph(attributePaths = ["spread", "spread.author", "card"])
    fun findByCard(card: Card): List<SpreadCard>

    @EntityGraph(attributePaths = ["card", "card.arcanaType"])
    fun findBySpreadAndPositionInSpread(
        spread: Spread,
        position: Int,
    ): SpreadCard?

    fun countBySpread(spread: Spread): Long

    @Modifying
    @Query("DELETE FROM SpreadCard sc WHERE sc.spread = :spread")
    fun deleteAllBySpread(
        @Param("spread") spread: Spread,
    )

    fun existsBySpreadAndCard(
        spread: Spread,
        card: Card,
    ): Boolean

    fun countBySpreadAndIsReversed(
        spread: Spread,
        isReversed: Boolean,
    ): Long

    @Query(
        """
        SELECT sc.spread FROM SpreadCard sc
        WHERE sc.isReversed = true
        GROUP BY sc.spread
        HAVING COUNT(sc) = :reversedCount
    """,
    )
    fun findSpreadsWithReversedCardCount(
        @Param("reversedCount") reversedCount: Long,
    ): List<Spread>

    @Query(
        """
        SELECT sc.card.id, COUNT(sc) as usage_count
        FROM SpreadCard sc
        GROUP BY sc.card.id
        ORDER BY usage_count DESC
    """,
    )
    fun getCardUsageStatistics(): List<Array<Any>>

    @Query(
        value = """
        SELECT sc1.card_id, sc2.card_id, COUNT(*) as pair_count
        FROM spread_card sc1
        JOIN spread_card sc2 ON sc1.spread_id = sc2.spread_id AND sc1.card_id < sc2.card_id
        GROUP BY sc1.card_id, sc2.card_id
        ORDER BY pair_count DESC
        LIMIT :limit
    """,
        nativeQuery = true,
    )
    fun findMostCommonCardPairs(
        @Param("limit") limit: Int,
    ): List<Array<Any>>
}
