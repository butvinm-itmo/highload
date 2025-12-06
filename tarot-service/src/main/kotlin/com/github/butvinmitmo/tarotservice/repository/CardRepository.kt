package com.github.butvinmitmo.tarotservice.repository

import com.github.butvinmitmo.tarotservice.entity.Card
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CardRepository : JpaRepository<Card, UUID> {
    @Query(value = "SELECT * FROM card ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    fun findRandomCards(
        @Param("limit") limit: Int,
    ): List<Card>
}
