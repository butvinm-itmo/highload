package com.github.butvinmitmo.divinationservice.mapper

import com.github.butvinmitmo.divinationservice.entity.Interpretation
import com.github.butvinmitmo.shared.client.UserServiceClient
import com.github.butvinmitmo.shared.dto.InterpretationDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class InterpretationMapper(
    private val userServiceClient: UserServiceClient,
) {
    // System context for internal service-to-service calls
    private val systemUserId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private val systemRole = "SYSTEM"

    fun toDto(interpretation: Interpretation): InterpretationDto {
        val author = userServiceClient.getUserById(systemUserId, systemRole, interpretation.authorId).body!!
        return InterpretationDto(
            id = interpretation.id!!,
            text = interpretation.text,
            author = author,
            spreadId = interpretation.spreadId,
            createdAt = interpretation.createdAt!!,
        )
    }
}
