package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.SpreadCard
import java.util.UUID

interface SpreadCardRepository {
    fun save(spreadCard: SpreadCard): SpreadCard
    fun findBySpreadId(spreadId: UUID): List<SpreadCard>
}
