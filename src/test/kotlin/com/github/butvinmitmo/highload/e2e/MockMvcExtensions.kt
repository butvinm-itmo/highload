package com.github.butvinmitmo.highload.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.UUID

// Extension functions for MockMvc to reduce boilerplate in e2e tests.

/**
 * Performs a POST request with JSON body.
 */
fun MockMvc.postJson(
    url: String,
    body: Any,
    objectMapper: ObjectMapper,
): ResultActions =
    this.perform(
        MockMvcRequestBuilders
            .post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)),
    )

/**
 * Performs a PUT request with JSON body.
 */
fun MockMvc.putJson(
    url: String,
    body: Any,
    objectMapper: ObjectMapper,
): ResultActions =
    this.perform(
        MockMvcRequestBuilders
            .put(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)),
    )

/**
 * Performs a DELETE request with JSON body.
 */
fun MockMvc.deleteJson(
    url: String,
    body: Any,
    objectMapper: ObjectMapper,
): ResultActions =
    this.perform(
        MockMvcRequestBuilders
            .delete(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)),
    )

/**
 * Extracts UUID from Location header (e.g., "/api/v0.0.1/users/{id}").
 * Use for endpoints that return Location header.
 */
fun ResultActions.getLocationId(): UUID {
    val location =
        this
            .andExpect(MockMvcResultMatchers.header().exists("Location"))
            .andReturn()
            .response
            .getHeader("Location")
            ?: throw IllegalStateException("Location header not found")

    val id = location.substringAfterLast("/")
    return UUID.fromString(id)
}

/**
 * Extracts UUID from JSON response body with "id" field.
 * Use for endpoints that return {"id": "uuid"} in response body.
 */
fun ResultActions.getIdFromBody(objectMapper: ObjectMapper): UUID {
    val response = this.andReturn().response.contentAsString
    val jsonNode = objectMapper.readTree(response)
    val idString = jsonNode.get("id").asText()
    return UUID.fromString(idString)
}
