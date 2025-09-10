package com.itmo.tarot.repository

import com.itmo.tarot.entity.ArcanaType
import com.itmo.tarot.entity.Card
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CardRepository : JpaRepository<Card, Int> {
    
    fun findByArcanaType(arcanaType: ArcanaType): List<Card>
    
    @Query("SELECT c FROM Card c ORDER BY FUNCTION('RANDOM')")
    fun findRandomCards(): List<Card>
}