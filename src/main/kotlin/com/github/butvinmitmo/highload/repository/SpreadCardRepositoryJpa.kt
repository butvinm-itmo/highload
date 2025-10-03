package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.SpreadCard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpreadCardRepositoryJpa :
    JpaRepository<SpreadCard, UUID>,
    SpreadCardRepository {

    @Query("SELECT sc FROM SpreadCard sc WHERE sc.spread.id = :spreadId ORDER BY sc.positionInSpread")
    override fun findBySpreadId(spreadId: UUID): List<SpreadCard>
}
