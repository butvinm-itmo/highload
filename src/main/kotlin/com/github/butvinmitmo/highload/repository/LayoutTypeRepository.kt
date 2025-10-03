package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.LayoutType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LayoutTypeRepository : JpaRepository<LayoutType, UUID> {
    fun findByName(name: String): LayoutType?
}
