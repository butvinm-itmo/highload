package com.github.butvinmitmo.divinationservice.repository

import com.github.butvinmitmo.divinationservice.entity.SpreadCard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpreadCardRepository : JpaRepository<SpreadCard, UUID>
