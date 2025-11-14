package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.SpreadCard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpreadCardRepository : JpaRepository<SpreadCard, UUID>
