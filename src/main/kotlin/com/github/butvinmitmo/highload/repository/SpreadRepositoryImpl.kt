package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.Spread
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class SpreadRepositoryImpl(
    private val jpaRepository: SpreadRepositoryJpa
) : SpreadRepository {

    override fun findById(id: UUID): Spread? {
        return jpaRepository.findById(id).orElse(null)
    }

    override fun findByIdWithCards(id: UUID): Spread? {
        return jpaRepository.findByIdWithCards(id)
    }

    override fun findByIdWithCardsAndInterpretations(id: UUID): Spread? {
        return jpaRepository.findByIdWithCardsAndInterpretations(id)
    }

    override fun save(spread: Spread): Spread {
        return jpaRepository.save(spread)
    }

    override fun findAllOrderByCreatedAtDesc(pageable: Pageable): Page<Spread> {
        return jpaRepository.findAllOrderByCreatedAtDesc(pageable)
    }

    override fun findSpreadsAfterCursor(spreadId: UUID, limit: Int): List<Spread> {
        return jpaRepository.findSpreadsAfterCursor(spreadId, limit)
    }

    override fun findLatestSpreads(limit: Int): List<Spread> {
        return jpaRepository.findLatestSpreads(limit)
    }

    override fun count(): Long {
        return jpaRepository.count()
    }

    override fun deleteById(id: UUID) {
        jpaRepository.deleteById(id)
    }
}