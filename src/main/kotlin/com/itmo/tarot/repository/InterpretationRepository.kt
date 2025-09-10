package com.itmo.tarot.repository

import com.itmo.tarot.entity.Interpretation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface InterpretationRepository : JpaRepository<Interpretation, Long> {
    
    fun findBySpreadId(spreadId: Long): List<Interpretation>
    
    @Query("""
        SELECT i FROM Interpretation i 
        WHERE i.author.id = :authorId AND i.spread.id = :spreadId
    """)
    fun findByAuthorIdAndSpreadId(
        @Param("authorId") authorId: Long, 
        @Param("spreadId") spreadId: Long
    ): Interpretation?
    
    fun findByAuthorId(authorId: Long): List<Interpretation>
}