package com.github.butvinmitmo.userservice.infrastructure.messaging.mapper

import com.github.butvinmitmo.shared.dto.events.UserEventData
import com.github.butvinmitmo.userservice.domain.model.User
import org.springframework.stereotype.Component

@Component
class UserEventDataMapper {
    fun toEventData(user: User): UserEventData =
        UserEventData(
            id = user.id!!,
            username = user.username,
            role = user.role.name,
            createdAt = user.createdAt!!,
        )
}
