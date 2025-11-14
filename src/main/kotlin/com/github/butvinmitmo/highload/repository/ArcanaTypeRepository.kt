package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.ArcanaType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ArcanaTypeRepository : JpaRepository<ArcanaType, UUID> {
    fun findByName(name: String): ArcanaType?
}
