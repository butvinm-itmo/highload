package com.github.butvinmitmo.userservice.infrastructure.external

import com.github.butvinmitmo.shared.client.DivinationServiceInternalClient
import com.github.butvinmitmo.userservice.application.interfaces.provider.DivinationServiceProvider
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class FeignDivinationServiceProvider(
    private val divinationServiceInternalClient: DivinationServiceInternalClient,
) : DivinationServiceProvider {
    override fun deleteUserData(userId: UUID) {
        divinationServiceInternalClient.deleteUserData(userId)
    }
}
