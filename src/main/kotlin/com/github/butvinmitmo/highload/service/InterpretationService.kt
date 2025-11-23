package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.dto.CreateInterpretationRequest
import com.github.butvinmitmo.highload.dto.CreateInterpretationResponse
import com.github.butvinmitmo.highload.dto.InterpretationDto
import com.github.butvinmitmo.highload.dto.UpdateInterpretationRequest
import com.github.butvinmitmo.highload.entity.Interpretation
import com.github.butvinmitmo.highload.exception.ConflictException
import com.github.butvinmitmo.highload.exception.ForbiddenException
import com.github.butvinmitmo.highload.exception.NotFoundException
import com.github.butvinmitmo.highload.mapper.InterpretationMapper
import com.github.butvinmitmo.highload.repository.InterpretationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class InterpretationService(
    private val interpretationRepository: InterpretationRepository,
    private val userService: UserService,
    private val spreadService: SpreadService,
    private val interpretationMapper: InterpretationMapper,
) {
    @Transactional
    fun addInterpretation(
        spreadId: UUID,
        request: CreateInterpretationRequest,
    ): CreateInterpretationResponse {
        val spread = spreadService.getSpreadEntity(spreadId)

        val user = userService.getUserEntity(request.authorId)

        if (interpretationRepository.existsByAuthorAndSpread(user.id, spreadId)) {
            throw ConflictException("You already have an interpretation for this spread")
        }

        val interpretation =
            Interpretation(
                text = request.text,
                author = user,
                spread = spread,
            )

        val saved = interpretationRepository.save(interpretation)
        return CreateInterpretationResponse(id = saved.id)
    }

    @Transactional
    fun updateInterpretation(
        spreadId: UUID,
        id: UUID,
        userId: UUID,
        request: UpdateInterpretationRequest,
    ): InterpretationDto {
        val interpretation =
            interpretationRepository
                .findById(id)
                .orElseThrow { NotFoundException("Interpretation not found") }

        if (interpretation.author.id != userId) {
            throw ForbiddenException("You can only edit your own interpretations")
        }

        interpretation.text = request.text
        val saved = interpretationRepository.save(interpretation)

        return interpretationMapper.toDto(saved)
    }

    @Transactional
    fun deleteInterpretation(
        spreadId: UUID,
        id: UUID,
        userId: UUID,
    ) {
        val interpretation =
            interpretationRepository
                .findById(id)
                .orElseThrow { NotFoundException("Interpretation not found") }

        if (interpretation.author.id != userId) {
            throw ForbiddenException("You can only delete your own interpretations")
        }

        interpretationRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun getInterpretation(
        spreadId: UUID,
        id: UUID,
    ): InterpretationDto {
        val interpretation =
            interpretationRepository
                .findById(id)
                .orElseThrow { NotFoundException("Interpretation not found") }

        if (interpretation.spread.id != spreadId) {
            throw NotFoundException("Interpretation not found in this spread")
        }

        return interpretationMapper.toDto(interpretation)
    }

    @Transactional(readOnly = true)
    fun getInterpretations(spreadId: UUID): List<InterpretationDto> {
        spreadService.getSpreadEntity(spreadId)
        return interpretationRepository
            .findBySpreadIdOrderByCreatedAtDesc(spreadId)
            .map { interpretationMapper.toDto(it) }
    }
}
