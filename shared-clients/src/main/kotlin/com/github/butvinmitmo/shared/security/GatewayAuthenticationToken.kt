package com.github.butvinmitmo.shared.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.UUID

class GatewayAuthenticationToken(
    val userId: UUID,
    val role: String,
) : AbstractAuthenticationToken(listOf(SimpleGrantedAuthority("ROLE_$role"))) {
    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any? = null

    override fun getPrincipal(): UUID = userId
}
