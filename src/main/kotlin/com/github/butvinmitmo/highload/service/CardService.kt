package com.github.butvinmitmo.highload.service

import com.github.butvinmitmo.highload.dto.ArcanaTypeDto
import com.github.butvinmitmo.highload.dto.CardDto
import com.github.butvinmitmo.highload.entity.ArcanaType
import com.github.butvinmitmo.highload.entity.Card
import com.github.butvinmitmo.highload.mapper.ArcanaTypeMapper
import com.github.butvinmitmo.highload.mapper.CardMapper
import com.github.butvinmitmo.highload.repository.ArcanaTypeRepository
import com.github.butvinmitmo.highload.repository.CardRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CardService(
    private val cardRepository: CardRepository,
    private val arcanaTypeRepository: ArcanaTypeRepository,
    private val cardMapper: CardMapper,
    private val arcanaTypeMapper: ArcanaTypeMapper,
) {
    @Transactional(readOnly = true)
    fun findRandomCards(count: Int): List<Card> {
        return cardRepository.findRandomCards(count)
    }

    @Transactional(readOnly = true)
    fun getAllCards(): List<CardDto> {
        return cardRepository.findAll()
            .map { cardMapper.toDto(it) }
    }

    @Transactional(readOnly = true)
    fun getAllArcanaTypes(): List<ArcanaTypeDto> {
        return arcanaTypeRepository.findAll()
            .map { arcanaTypeMapper.toDto(it) }
    }

    @Transactional(readOnly = true)
    fun getArcanaTypeByName(name: String): ArcanaType? {
        return arcanaTypeRepository.findByName(name)
    }
}