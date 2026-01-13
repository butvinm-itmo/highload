package com.github.butvinmitmo.shared.client

import com.github.butvinmitmo.shared.dto.AuthTokenResponse
import com.github.butvinmitmo.shared.dto.CardDto
import com.github.butvinmitmo.shared.dto.CreateUserRequest
import com.github.butvinmitmo.shared.dto.CreateUserResponse
import com.github.butvinmitmo.shared.dto.LayoutTypeDto
import com.github.butvinmitmo.shared.dto.LoginRequest
import com.github.butvinmitmo.shared.dto.UpdateUserRequest
import com.github.butvinmitmo.shared.dto.UserDto
import org.springframework.cloud.openfeign.FallbackFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserServiceFallbackFactory : FallbackFactory<UserServiceClient> {
    override fun create(cause: Throwable): UserServiceClient =
        object : UserServiceClient {
            override fun login(request: LoginRequest): ResponseEntity<AuthTokenResponse> =
                throw ServiceUnavailableException("user-service", cause)

            override fun createUser(
                userId: UUID,
                role: String,
                request: CreateUserRequest,
            ): ResponseEntity<CreateUserResponse> = throw ServiceUnavailableException("user-service", cause)

            override fun getUsers(
                userId: UUID,
                role: String,
                page: Int,
                size: Int,
            ): ResponseEntity<List<UserDto>> = throw ServiceUnavailableException("user-service", cause)

            override fun getUserById(
                userId: UUID,
                role: String,
                id: UUID,
            ): ResponseEntity<UserDto> = throw ServiceUnavailableException("user-service", cause)

            override fun updateUser(
                userId: UUID,
                role: String,
                id: UUID,
                request: UpdateUserRequest,
            ): ResponseEntity<UserDto> = throw ServiceUnavailableException("user-service", cause)

            override fun deleteUser(
                userId: UUID,
                role: String,
                id: UUID,
            ): ResponseEntity<Void> = throw ServiceUnavailableException("user-service", cause)
        }
}

@Component
class TarotServiceFallbackFactory : FallbackFactory<TarotServiceClient> {
    override fun create(cause: Throwable): TarotServiceClient =
        object : TarotServiceClient {
            override fun getCards(
                userId: UUID,
                role: String,
                page: Int,
                size: Int,
            ): ResponseEntity<List<CardDto>> = throw ServiceUnavailableException("tarot-service", cause)

            override fun getLayoutTypes(
                userId: UUID,
                role: String,
                page: Int,
                size: Int,
            ): ResponseEntity<List<LayoutTypeDto>> = throw ServiceUnavailableException("tarot-service", cause)

            override fun getRandomCards(
                userId: UUID,
                role: String,
                count: Int,
            ): ResponseEntity<List<CardDto>> = throw ServiceUnavailableException("tarot-service", cause)

            override fun getLayoutTypeById(
                userId: UUID,
                role: String,
                id: UUID,
            ): ResponseEntity<LayoutTypeDto> = throw ServiceUnavailableException("tarot-service", cause)
        }
}

@Component
class DivinationServiceInternalFallbackFactory : FallbackFactory<DivinationServiceInternalClient> {
    override fun create(cause: Throwable): DivinationServiceInternalClient =
        object : DivinationServiceInternalClient {
            override fun deleteUserData(userId: UUID): ResponseEntity<Void> =
                throw ServiceUnavailableException("divination-service", cause)
        }
}
