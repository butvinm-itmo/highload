package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.Interpretation
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class InterpretationRepositoryImpl(
    private val jpaRepository: InterpretationRepositoryJpa
) : InterpretationRepository {

    override fun findById(id: UUID): Interpretation? {
        return jpaRepository.findById(id).orElse(null)
    }

    override fun existsByAuthorAndSpread(authorId: UUID, spreadId: UUID): Boolean {
        return jpaRepository.existsByAuthorAndSpread(authorId, spreadId)
    }

    override fun save(interpretation: Interpretation): Interpretation {
        return jpaRepository.save(interpretation)
    }

    override fun deleteById(id: UUID) {
        jpaRepository.deleteById(id)
    }
}