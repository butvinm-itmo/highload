package com.itmo.tarot.unit

import com.itmo.tarot.dto.request.CreateSpreadRequest
import com.itmo.tarot.entity.*
import com.itmo.tarot.exception.SpreadNotFoundException
import com.itmo.tarot.exception.UnauthorizedOperationException
import com.itmo.tarot.repository.SpreadRepository
import com.itmo.tarot.service.CardService
import com.itmo.tarot.service.SpreadService
import com.itmo.tarot.service.UserService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

class SpreadServiceTest {
    
    private val spreadRepository = mockk<SpreadRepository>()
    private val userService = mockk<UserService>()
    private val cardService = mockk<CardService>()
    
    private val spreadService = SpreadService(spreadRepository, userService, cardService)
    
    @Test
    fun `should create spread successfully`() {
        val userId = 1L
        val user = User(id = userId)
        val cards = listOf(
            Card(id = 1, name = "The Fool", arcanaType = ArcanaType.MAJOR),
            Card(id = 2, name = "The Magician", arcanaType = ArcanaType.MAJOR),
            Card(id = 3, name = "The High Priestess", arcanaType = ArcanaType.MAJOR)
        )
        
        val request = CreateSpreadRequest(
            userId = userId,
            question = "What does the future hold?",
            layoutType = LayoutType.THREE_CARDS
        )
        
        val savedSpread = Spread(
            id = 1L,
            question = request.question,
            layoutType = request.layoutType,
            author = user,
            createdAt = LocalDateTime.now()
        )
        
        val spreadSlot = slot<Spread>()
        
        every { userService.findOrCreateUser(userId) } returns user
        every { cardService.generateCardsForLayout(LayoutType.THREE_CARDS) } returns cards
        every { spreadRepository.save(capture(spreadSlot)) } returns savedSpread
        
        val result = spreadService.createSpread(request)
        
        assertNotNull(result)
        assertEquals(request.question, result.question)
        assertEquals(request.layoutType, result.layoutType)
        assertEquals(userId, result.authorId)
        assertEquals(3, result.cards.size)
        
        verify { userService.findOrCreateUser(userId) }
        verify { cardService.generateCardsForLayout(LayoutType.THREE_CARDS) }
        verify(exactly = 2) { spreadRepository.save(any()) }
    }
    
    @Test
    fun `should throw exception when spread not found`() {
        val spreadId = 999L
        
        every { spreadRepository.findByIdWithDetails(spreadId) } returns null
        
        assertThrows<SpreadNotFoundException> {
            spreadService.findById(spreadId)
        }
        
        verify { spreadRepository.findByIdWithDetails(spreadId) }
    }
    
    @Test
    fun `should delete spread when user is authorized`() {
        val spreadId = 1L
        val userId = 1L
        val user = User(id = userId)
        val spread = Spread(
            id = spreadId,
            question = "Test question",
            layoutType = LayoutType.ONE_CARD,
            author = user
        )
        
        every { spreadRepository.findById(spreadId) } returns Optional.of(spread)
        every { spreadRepository.deleteById(spreadId) } returns Unit
        
        assertDoesNotThrow {
            spreadService.deleteSpread(spreadId, userId)
        }
        
        verify { spreadRepository.findById(spreadId) }
        verify { spreadRepository.deleteById(spreadId) }
    }
    
    @Test
    fun `should throw exception when user is not authorized to delete spread`() {
        val spreadId = 1L
        val authorId = 1L
        val requestingUserId = 2L
        
        val author = User(id = authorId)
        val spread = Spread(
            id = spreadId,
            question = "Protected spread",
            layoutType = LayoutType.ONE_CARD,
            author = author
        )
        
        every { spreadRepository.findById(spreadId) } returns Optional.of(spread)
        
        assertThrows<UnauthorizedOperationException> {
            spreadService.deleteSpread(spreadId, requestingUserId)
        }
        
        verify { spreadRepository.findById(spreadId) }
        verify(exactly = 0) { spreadRepository.deleteById(any()) }
    }
    
    @Test
    fun `should throw exception when trying to delete non-existent spread`() {
        val spreadId = 999L
        val userId = 1L
        
        every { spreadRepository.findById(spreadId) } returns Optional.empty()
        
        assertThrows<SpreadNotFoundException> {
            spreadService.deleteSpread(spreadId, userId)
        }
        
        verify { spreadRepository.findById(spreadId) }
        verify(exactly = 0) { spreadRepository.deleteById(any()) }
    }
}