package com.itmo.tarot.service

import com.itmo.tarot.entity.User
import com.itmo.tarot.exception.UserNotFoundException
import com.itmo.tarot.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository
) {
    
    fun findOrCreateUser(id: Long): User {
        return userRepository.findById(id)
            .orElseGet { 
                userRepository.save(User(id = id))
            }
    }
    
    fun findById(id: Long): User {
        return userRepository.findById(id)
            .orElseThrow { UserNotFoundException(id) }
    }
    
    @Transactional
    fun deleteUser(id: Long) {
        if (!userRepository.existsById(id)) {
            throw UserNotFoundException(id)
        }
        
        userRepository.deleteById(id)
    }
}