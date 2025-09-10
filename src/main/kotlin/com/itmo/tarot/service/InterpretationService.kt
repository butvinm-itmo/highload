package com.itmo.tarot.service

import com.itmo.tarot.dto.request.CreateInterpretationRequest
import com.itmo.tarot.dto.request.UpdateInterpretationRequest
import com.itmo.tarot.dto.response.InterpretationResponse
import com.itmo.tarot.entity.Interpretation
import com.itmo.tarot.exception.InterpretationAlreadyExistsException
import com.itmo.tarot.exception.InterpretationNotFoundException
import com.itmo.tarot.exception.SpreadNotFoundException
import com.itmo.tarot.exception.UnauthorizedOperationException
import com.itmo.tarot.repository.InterpretationRepository
import com.itmo.tarot.repository.SpreadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InterpretationService(
    private val interpretationRepository: InterpretationRepository,
    private val spreadRepository: SpreadRepository,
    private val userService: UserService
) {
    
    @Transactional
    fun createInterpretation(spreadId: Long, request: CreateInterpretationRequest): InterpretationResponse {
        val spread = spreadRepository.findById(spreadId)
            .orElseThrow { SpreadNotFoundException(spreadId) }
        
        val user = userService.findOrCreateUser(request.userId)
        
        val existingInterpretation = interpretationRepository
            .findByAuthorIdAndSpreadId(request.userId, spreadId)
        
        if (existingInterpretation != null) {
            throw InterpretationAlreadyExistsException(request.userId, spreadId)
        }
        
        val interpretation = Interpretation(
            text = request.text,
            author = user,
            spread = spread
        )
        
        val savedInterpretation = interpretationRepository.save(interpretation)
        
        return mapToInterpretationResponse(savedInterpretation)
    }
    
    @Transactional
    fun updateInterpretation(
        spreadId: Long,
        interpretationId: Long,
        request: UpdateInterpretationRequest
    ): InterpretationResponse {
        val interpretation = interpretationRepository.findById(interpretationId)
            .orElseThrow { InterpretationNotFoundException(interpretationId) }
        
        if (interpretation.spread.id != spreadId) {
            throw InterpretationNotFoundException(interpretationId)
        }
        
        if (interpretation.author.id != request.userId) {
            throw UnauthorizedOperationException(
                "User ${request.userId} is not authorized to update interpretation $interpretationId"
            )
        }
        
        val updatedInterpretation = interpretation.copy(text = request.text)
        val savedInterpretation = interpretationRepository.save(updatedInterpretation)
        
        return mapToInterpretationResponse(savedInterpretation)
    }
    
    @Transactional
    fun deleteInterpretation(spreadId: Long, interpretationId: Long, userId: Long) {
        val interpretation = interpretationRepository.findById(interpretationId)
            .orElseThrow { InterpretationNotFoundException(interpretationId) }
        
        if (interpretation.spread.id != spreadId) {
            throw InterpretationNotFoundException(interpretationId)
        }
        
        if (interpretation.author.id != userId) {
            throw UnauthorizedOperationException(
                "User $userId is not authorized to delete interpretation $interpretationId"
            )
        }
        
        interpretationRepository.deleteById(interpretationId)
    }
    
    fun findBySpreadId(spreadId: Long): List<InterpretationResponse> {
        return interpretationRepository.findBySpreadId(spreadId)
            .map { mapToInterpretationResponse(it) }
    }
    
    private fun mapToInterpretationResponse(interpretation: Interpretation): InterpretationResponse {
        return InterpretationResponse(
            id = interpretation.id!!,
            text = interpretation.text,
            createdAt = interpretation.createdAt,
            authorId = interpretation.author.id
        )
    }
}