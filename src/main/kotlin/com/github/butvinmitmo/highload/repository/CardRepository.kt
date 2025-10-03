package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.Card

interface CardRepository {
    fun findRandomCards(limit: Int): List<Card>
}
