package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.Card
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CardRepositoryJpa :
    JpaRepository<Card, UUID>,
    CardRepository {
    @Query(value = "SELECT * FROM card ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    override fun findRandomCards(
        @Param("limit") limit: Int,
    ): List<Card>
}
