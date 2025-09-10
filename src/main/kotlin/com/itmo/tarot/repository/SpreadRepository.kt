package com.itmo.tarot.repository

import com.itmo.tarot.entity.Spread
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SpreadRepository : JpaRepository<Spread, Long> {
    
    @Query("""
        SELECT s FROM Spread s 
        LEFT JOIN FETCH s.spreadCards sc 
        LEFT JOIN FETCH sc.card 
        LEFT JOIN FETCH s.interpretations i 
        LEFT JOIN FETCH i.author
        WHERE s.id = :id
    """)
    fun findByIdWithDetails(@Param("id") id: Long): Spread?
    
    @Query("""
        SELECT s FROM Spread s 
        ORDER BY s.createdAt DESC
    """)
    override fun findAll(pageable: Pageable): Page<Spread>
    
    @Query("""
        SELECT s FROM Spread s 
        WHERE (:afterId IS NULL OR s.id < :afterId)
        ORDER BY s.createdAt DESC, s.id DESC
    """)
    fun findAllForInfiniteScroll(
        @Param("afterId") afterId: Long?, 
        pageable: Pageable
    ): List<Spread>
    
    fun findByAuthorId(authorId: Long): List<Spread>
}