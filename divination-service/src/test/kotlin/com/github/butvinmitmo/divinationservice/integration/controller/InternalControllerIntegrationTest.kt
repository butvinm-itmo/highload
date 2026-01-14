package com.github.butvinmitmo.divinationservice.integration.controller

import com.github.butvinmitmo.divinationservice.entity.Interpretation
import com.github.butvinmitmo.divinationservice.entity.Spread
import com.github.butvinmitmo.divinationservice.entity.SpreadCard
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class InternalControllerIntegrationTest : BaseControllerIntegrationTest() {
    @Test
    fun `deleteUserData should delete all spreads and interpretations for a user`() {
        // Given: a user with spreads and interpretations
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()

        // Create spread for user
        val spread1 =
            spreadRepository
                .save(
                    Spread(
                        question = "Test question",
                        authorId = userId,
                        layoutTypeId = oneCardLayoutId,
                    ),
                ).block()!!

        // Create spread cards for spread1
        spreadCardRepository
            .save(
                SpreadCard(
                    spreadId = spread1.id!!,
                    cardId = UUID.fromString("00000000-0000-0000-0000-000000000030"),
                    positionInSpread = 1,
                    isReversed = false,
                ),
            ).block()

        // Create another spread for same user
        val spread2 =
            spreadRepository
                .save(
                    Spread(
                        question = "Another question",
                        authorId = userId,
                        layoutTypeId = oneCardLayoutId,
                    ),
                ).block()!!

        // Create spread for other user
        val otherUserSpread =
            spreadRepository
                .save(
                    Spread(
                        question = "Other user question",
                        authorId = otherUserId,
                        layoutTypeId = oneCardLayoutId,
                    ),
                ).block()!!

        // Create interpretation by userId on spread1
        interpretationRepository
            .save(
                Interpretation(
                    text = "User interpretation",
                    authorId = userId,
                    spreadId = spread1.id!!,
                ),
            ).block()

        // Create interpretation by otherUser on spread1
        interpretationRepository
            .save(
                Interpretation(
                    text = "Other user interpretation on spread1",
                    authorId = otherUserId,
                    spreadId = spread1.id!!,
                ),
            ).block()

        // Create interpretation by userId on otherUser's spread
        interpretationRepository
            .save(
                Interpretation(
                    text = "User interpretation on other spread",
                    authorId = userId,
                    spreadId = otherUserSpread.id!!,
                ),
            ).block()

        // Create interpretation by otherUser on their own spread (this should remain)
        interpretationRepository
            .save(
                Interpretation(
                    text = "Other user interpretation on their own spread",
                    authorId = otherUserId,
                    spreadId = otherUserSpread.id!!,
                ),
            ).block()

        // Verify initial state
        assertEquals(3, spreadRepository.count().block())
        assertEquals(
            4,
            interpretationRepository
                .findAll()
                .collectList()
                .block()
                ?.size,
        )

        // When: delete user data
        webTestClient
            .delete()
            .uri("/internal/users/$userId/data")
            .exchange()
            .expectStatus()
            .isNoContent

        // Then: user's spreads are deleted (cascade deletes spread_cards)
        val remainingSpreads = spreadRepository.findAll().collectList().block()!!
        assertEquals(1, remainingSpreads.size)
        assertEquals(otherUserId, remainingSpreads[0].authorId)

        // And: user's interpretations are deleted, but other user's interpretation on other spread remains
        val remainingInterpretations = interpretationRepository.findAll().collectList().block()!!
        assertEquals(1, remainingInterpretations.size)
        assertEquals(otherUserId, remainingInterpretations[0].authorId)
    }

    @Test
    fun `deleteUserData should return 204 even if user has no data`() {
        // Given: a user with no spreads or interpretations
        val userId = UUID.randomUUID()

        // When: delete user data
        webTestClient
            .delete()
            .uri("/internal/users/$userId/data")
            .exchange()
            .expectStatus()
            .isNoContent

        // Then: no error occurred
        assertTrue(true)
    }

    @Test
    fun `deleteUserData should cascade delete spread cards when spread is deleted`() {
        // Given: a user with a spread and spread cards
        val userId = UUID.randomUUID()

        val spread =
            spreadRepository
                .save(
                    Spread(
                        question = "Test question",
                        authorId = userId,
                        layoutTypeId = threeCardsLayoutId,
                    ),
                ).block()!!

        // Create 3 spread cards
        repeat(3) { index ->
            spreadCardRepository
                .save(
                    SpreadCard(
                        spreadId = spread.id!!,
                        cardId = UUID.fromString("00000000-0000-0000-0000-00000000003$index"),
                        positionInSpread = index + 1,
                        isReversed = false,
                    ),
                ).block()
        }

        assertEquals(
            3,
            spreadCardRepository
                .findBySpreadId(spread.id!!)
                .collectList()
                .block()
                ?.size,
        )

        // When: delete user data
        webTestClient
            .delete()
            .uri("/internal/users/$userId/data")
            .exchange()
            .expectStatus()
            .isNoContent

        // Then: spread and all spread cards are deleted
        assertEquals(0, spreadRepository.count().block())
        assertEquals(0, spreadCardRepository.count().block())
    }
}
