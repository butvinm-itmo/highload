package com.github.butvinmitmo.sharedsecurity

import java.util.UUID

/**
 * Principal representing an authenticated user extracted from gateway headers.
 *
 * @property userId The unique identifier of the user (from X-User-Id header)
 * @property role The role of the user (from X-User-Role header): USER, MEDIUM, or ADMIN
 */
data class UserPrincipal(
    val userId: UUID,
    val role: String,
)
