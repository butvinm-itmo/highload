package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.ArcanaType
import com.github.butvinmitmo.highload.entity.Card
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface CardRepository : JpaRepository<Card, UUID> {
    @EntityGraph(attributePaths = ["arcanaType"])
    override fun findById(id: UUID): Optional<Card>

    @EntityGraph(attributePaths = ["arcanaType"])
    override fun findAll(): List<Card>

    @EntityGraph(attributePaths = ["arcanaType"])
    override fun findAll(pageable: Pageable): Page<Card>

    @EntityGraph(attributePaths = ["arcanaType"])
    fun findByArcanaType(arcanaType: ArcanaType): List<Card>

    @EntityGraph(attributePaths = ["arcanaType"])
    fun findByArcanaType(
        arcanaType: ArcanaType,
        pageable: Pageable,
    ): Page<Card>

    @EntityGraph(attributePaths = ["arcanaType"])
    fun findByName(name: String): Optional<Card>

    @EntityGraph(attributePaths = ["arcanaType"])
    fun findByNameContainingIgnoreCase(
        searchTerm: String,
        pageable: Pageable,
    ): Page<Card>

    @Query(value = "SELECT * FROM card ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    fun findRandomCards(limit: Int): List<Card>

    @Query(
        value = "SELECT * FROM card WHERE arcana_type_id = :arcanaTypeId ORDER BY RANDOM() LIMIT :limit",
        nativeQuery = true,
    )
    fun findRandomCardsByArcanaType(
        arcanaTypeId: UUID,
        limit: Int,
    ): List<Card>

    fun countByArcanaType(arcanaType: ArcanaType): Long

    fun existsByName(name: String): Boolean
}
