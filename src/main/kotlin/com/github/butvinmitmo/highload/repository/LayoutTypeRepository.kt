package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.LayoutType

interface LayoutTypeRepository {
    fun findByName(name: String): LayoutType?
}
