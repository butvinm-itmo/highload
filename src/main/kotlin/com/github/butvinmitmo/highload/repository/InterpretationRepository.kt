package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.Interpretation
import java.util.UUID

interface InterpretationRepository {
    fun findById(id: UUID): Interpretation?

    fun existsByAuthorAndSpread(
        authorId: UUID,
        spreadId: UUID,
    ): Boolean

    fun save(interpretation: Interpretation): Interpretation

    fun deleteById(id: UUID)
}
