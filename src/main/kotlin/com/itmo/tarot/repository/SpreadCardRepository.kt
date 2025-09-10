package com.itmo.tarot.repository

import com.itmo.tarot.entity.SpreadCard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SpreadCardRepository : JpaRepository<SpreadCard, Long> {
    
    fun findBySpreadIdOrderByPositionInSpread(spreadId: Long): List<SpreadCard>
    
    fun deleteBySpreadId(spreadId: Long)
}