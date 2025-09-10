package com.itmo.tarot.repository

import com.itmo.tarot.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    
    @Modifying
    @Query("DELETE FROM User u WHERE u.id = :userId")
    fun deleteByIdWithCascade(@Param("userId") userId: Long)
}