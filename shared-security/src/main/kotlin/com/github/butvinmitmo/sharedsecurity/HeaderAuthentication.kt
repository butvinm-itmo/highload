package com.github.butvinmitmo.sharedsecurity

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority

/**
 * Authentication token created from gateway-provided headers (X-User-Id, X-User-Role).
 *
 * This authentication is pre-validated by the gateway, so it's marked as authenticated
 * immediately upon creation.
 */
class HeaderAuthentication(
    private val userPrincipal: UserPrincipal,
) : AbstractAuthenticationToken(listOf(SimpleGrantedAuthority("ROLE_${userPrincipal.role}"))) {
    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any? = null

    override fun getPrincipal(): UserPrincipal = userPrincipal
}
