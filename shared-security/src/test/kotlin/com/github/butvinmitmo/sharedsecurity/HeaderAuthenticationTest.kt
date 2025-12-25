package com.github.butvinmitmo.sharedsecurity

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeaderAuthenticationTest {
    @Test
    fun `should create authenticated token with ADMIN role`() {
        val userId = UUID.randomUUID()
        val principal = UserPrincipal(userId, "ADMIN")
        val auth = HeaderAuthentication(principal)

        assertTrue(auth.isAuthenticated)
        assertEquals(principal, auth.principal)
        assertEquals(userId, auth.principal.userId)
        assertEquals("ADMIN", auth.principal.role)
        assertNull(auth.credentials)
    }

    @Test
    fun `should have correct authority for ADMIN role`() {
        val principal = UserPrincipal(UUID.randomUUID(), "ADMIN")
        val auth = HeaderAuthentication(principal)

        assertEquals(1, auth.authorities.size)
        assertEquals("ROLE_ADMIN", auth.authorities.first().authority)
    }

    @Test
    fun `should have correct authority for USER role`() {
        val principal = UserPrincipal(UUID.randomUUID(), "USER")
        val auth = HeaderAuthentication(principal)

        assertEquals(1, auth.authorities.size)
        assertEquals("ROLE_USER", auth.authorities.first().authority)
    }

    @Test
    fun `should have correct authority for MEDIUM role`() {
        val principal = UserPrincipal(UUID.randomUUID(), "MEDIUM")
        val auth = HeaderAuthentication(principal)

        assertEquals(1, auth.authorities.size)
        assertEquals("ROLE_MEDIUM", auth.authorities.first().authority)
    }
}
