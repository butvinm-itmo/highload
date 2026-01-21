package com.github.butvinmitmo.tarotservice.infrastructure.persistence.repository

import com.github.butvinmitmo.tarotservice.infrastructure.persistence.entity.ArcanaTypeEntity
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataArcanaTypeRepository : R2dbcRepository<ArcanaTypeEntity, UUID>
