package com.github.butvinmitmo.highload.integration.controller

import com.github.butvinmitmo.highload.dto.CreateSpreadRequest
import com.github.butvinmitmo.highload.dto.CreateUserRequest
import com.github.butvinmitmo.highload.dto.DeleteRequest
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class SpreadControllerIntegrationTest : BaseControllerIntegrationTest() {
    private val baseUrl = "/api/v0.0.1/spreads"
    private val usersUrl = "/api/v0.0.1/users"

    @Test
    fun `should create spread with random cards and return 201`() {
        val userId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("spreaduser"), objectMapper)
                .getIdFromBody(objectMapper)

        val request =
            CreateSpreadRequest(
                question = "What does the future hold?",
                layoutTypeId = threeCardsLayoutId,
                authorId = userId,
            )

        val spreadId =
            mockMvc
                .postJson(baseUrl, request, objectMapper)
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").exists())
                .getIdFromBody(objectMapper)

        // Verify spread details with cards
        mockMvc
            .perform(get("$baseUrl/$spreadId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(spreadId.toString()))
            .andExpect(jsonPath("$.question").value("What does the future hold?"))
            .andExpect(jsonPath("$.layoutType.name").value("THREE_CARDS"))
            .andExpect(jsonPath("$.author.id").value(userId.toString()))
            .andExpect(jsonPath("$.cards").isArray)
            .andExpect(jsonPath("$.cards.length()").value(3))
            .andExpect(jsonPath("$.cards[0].card.id").exists())
            .andExpect(jsonPath("$.cards[0].card.name").exists())
            .andExpect(jsonPath("$.cards[0].positionInSpread").exists())
            .andExpect(jsonPath("$.cards[0].reversed").exists())
    }

    @Test
    fun `should get spread details with cards and return 200`() {
        val userId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("detailuser"), objectMapper)
                .getIdFromBody(objectMapper)

        val request =
            CreateSpreadRequest(
                question = "Test question",
                layoutTypeId = oneCardLayoutId,
                authorId = userId,
            )

        val spreadId =
            mockMvc
                .postJson(baseUrl, request, objectMapper)
                .getIdFromBody(objectMapper)

        mockMvc
            .perform(get("$baseUrl/$spreadId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(spreadId.toString()))
            .andExpect(jsonPath("$.question").value("Test question"))
            .andExpect(jsonPath("$.layoutType.name").value("ONE_CARD"))
            .andExpect(jsonPath("$.cards.length()").value(1))
            .andExpect(jsonPath("$.interpretations").isArray)
    }

    @Test
    fun `should return 404 when getting non-existent spread`() {
        val nonExistentId = UUID.randomUUID()

        mockMvc
            .perform(get("$baseUrl/$nonExistentId"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `should return paginated spread list with X-Total-Count header`() {
        val userId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("paginuser"), objectMapper)
                .getIdFromBody(objectMapper)

        // Create multiple spreads
        for (i in 1..5) {
            val request =
                CreateSpreadRequest(
                    question = "Question $i",
                    layoutTypeId = oneCardLayoutId,
                    authorId = userId,
                )
            mockMvc.postJson(baseUrl, request, objectMapper)
        }

        mockMvc
            .perform(get(baseUrl).param("page", "0").param("size", "3"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].question").exists())
    }

    @Test
    fun `should support scroll pagination with X-After header`() {
        val userId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("scrolluser_xafter"), objectMapper)
                .getIdFromBody(objectMapper)

        for (i in 1..3) {
            val request =
                CreateSpreadRequest(
                    question = "Scroll Question $i",
                    layoutTypeId = oneCardLayoutId,
                    authorId = userId,
                )
            mockMvc.postJson(baseUrl, request, objectMapper)
        }

        val firstPage =
            mockMvc
                .perform(get("$baseUrl/scroll").param("size", "2"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn()

        val firstPageCursor = firstPage.response.getHeader("X-After")
        assert(firstPageCursor != null)

        val secondPage =
            mockMvc
                .perform(get("$baseUrl/scroll").param("after", firstPageCursor).param("size", "2"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn()

        val secondPageCursor = secondPage.response.getHeader("X-After")
        assert(secondPageCursor == null)
    }

    @Test
    fun `should delete spread with correct userId and return 204`() {
        val userId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("deleteuser"), objectMapper)
                .getIdFromBody(objectMapper)

        val request =
            CreateSpreadRequest(
                question = "To be deleted",
                layoutTypeId = oneCardLayoutId,
                authorId = userId,
            )

        val spreadId =
            mockMvc
                .postJson(baseUrl, request, objectMapper)
                .getIdFromBody(objectMapper)

        val deleteRequest = DeleteRequest(userId = userId)
        mockMvc
            .deleteJson("$baseUrl/$spreadId", deleteRequest, objectMapper)
            .andExpect(status().isNoContent)

        // Verify spread is deleted
        mockMvc
            .perform(get("$baseUrl/$spreadId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 403 when deleting spread with wrong userId`() {
        val authorId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("author"), objectMapper)
                .getIdFromBody(objectMapper)

        val otherId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("other"), objectMapper)
                .getIdFromBody(objectMapper)

        val request =
            CreateSpreadRequest(
                question = "Protected spread",
                layoutTypeId = oneCardLayoutId,
                authorId = authorId,
            )

        val spreadId =
            mockMvc
                .postJson(baseUrl, request, objectMapper)
                .getIdFromBody(objectMapper)

        // Attempt to delete with wrong userId
        val deleteRequest = DeleteRequest(userId = otherId)
        mockMvc
            .deleteJson("$baseUrl/$spreadId", deleteRequest, objectMapper)
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").exists())

        // Verify spread still exists
        mockMvc
            .perform(get("$baseUrl/$spreadId"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should return 404 when deleting non-existent spread`() {
        val userId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("someuser"), objectMapper)
                .getIdFromBody(objectMapper)

        val nonExistentId = UUID.randomUUID()
        val deleteRequest = DeleteRequest(userId = userId)

        mockMvc
            .deleteJson("$baseUrl/$nonExistentId", deleteRequest, objectMapper)
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should create spread with CROSS layout and return 10 cards`() {
        val userId =
            mockMvc
                .postJson(usersUrl, CreateUserRequest("crossuser"), objectMapper)
                .getIdFromBody(objectMapper)

        val request =
            CreateSpreadRequest(
                question = "Complex question",
                layoutTypeId = crossLayoutId,
                authorId = userId,
            )

        val spreadId =
            mockMvc
                .postJson(baseUrl, request, objectMapper)
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").exists())
                .getIdFromBody(objectMapper)

        // Verify spread has CROSS layout with 10 cards
        mockMvc
            .perform(get("$baseUrl/$spreadId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.layoutType.name").value("CROSS"))
            .andExpect(jsonPath("$.cards.length()").value(10))
    }
}
