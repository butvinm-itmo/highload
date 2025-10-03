package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.Spread
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface SpreadRepository {
    fun findById(id: UUID): Spread?

    fun findByIdWithCards(id: UUID): Spread?

    fun findByIdWithCardsAndInterpretations(id: UUID): Spread?

    fun save(spread: Spread): Spread

    fun findAllOrderByCreatedAtDesc(pageable: Pageable): Page<Spread>

    fun findSpreadsAfterCursor(
        spreadId: UUID,
        limit: Int,
    ): List<Spread>

    fun findLatestSpreads(limit: Int): List<Spread>

    fun count(): Long

    fun deleteById(id: UUID)
}
