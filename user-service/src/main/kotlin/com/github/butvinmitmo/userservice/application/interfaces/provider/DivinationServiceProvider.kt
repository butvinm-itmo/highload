package com.github.butvinmitmo.userservice.application.interfaces.provider

import java.util.UUID

interface DivinationServiceProvider {
    fun deleteUserData(userId: UUID)
}
