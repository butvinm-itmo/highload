package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.dto.CreateInterpretationRequest
import com.github.butvinmitmo.highload.dto.InterpretationDto
import com.github.butvinmitmo.highload.dto.UpdateInterpretationRequest
import com.github.butvinmitmo.highload.entity.Interpretation
import com.github.butvinmitmo.highload.exception.ConflictException
import com.github.butvinmitmo.highload.exception.ForbiddenException
import com.github.butvinmitmo.highload.exception.NotFoundException
import com.github.butvinmitmo.highload.mapper.InterpretationMapper
import com.github.butvinmitmo.highload.repository.InterpretationRepository
import com.github.butvinmitmo.highload.repository.SpreadRepository
import com.github.butvinmitmo.highload.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class InterpretationService(
    private val interpretationRepository: InterpretationRepository,
    private val spreadRepository: SpreadRepository,
    private val userRepository: UserRepository,
    private val interpretationMapper: InterpretationMapper,
) {
    @Transactional
    fun addInterpretation(
        spreadId: UUID,
        request: CreateInterpretationRequest,
    ): InterpretationDto {
        // 1. Validate spread exists
        val spread =
            spreadRepository.findById(spreadId)
                ?: throw NotFoundException("Spread not found")

        // 2. Validate user exists
        val user =
            userRepository.findById(request.authorId)
                ?: throw NotFoundException("User not found")

        // 3. Check if user already has interpretation for this spread
        if (interpretationRepository.existsByAuthorAndSpread(user.id!!, spreadId)) {
            throw ConflictException("You already have an interpretation for this spread")
        }

        // 4. Create interpretation
        val interpretation =
            Interpretation(
                text = request.text,
                author = user,
                spread = spread,
            )

        val saved = interpretationRepository.save(interpretation)
        return interpretationMapper.toDto(saved)
    }

    @Transactional
    fun updateInterpretation(
        spreadId: UUID,
        id: UUID,
        userId: UUID,
        request: UpdateInterpretationRequest,
    ): InterpretationDto {
        val interpretation =
            interpretationRepository.findById(id)
                ?: throw NotFoundException("Interpretation not found")

        if (interpretation.author.id != userId) {
            throw ForbiddenException("You can only edit your own interpretations")
        }

        // Create a new interpretation with updated text
        val updated = interpretation.copy(text = request.text)
        val saved = interpretationRepository.save(updated)

        return interpretationMapper.toDto(saved)
    }

    @Transactional
    fun deleteInterpretation(
        spreadId: UUID,
        id: UUID,
        userId: UUID,
    ) {
        val interpretation =
            interpretationRepository.findById(id)
                ?: throw NotFoundException("Interpretation not found")

        if (interpretation.author.id != userId) {
            throw ForbiddenException("You can only delete your own interpretations")
        }

        interpretationRepository.deleteById(id)
    }
}
