package com.github.butvinmitmo.userservice.infrastructure.security

import com.github.butvinmitmo.userservice.application.interfaces.provider.PasswordEncoder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class SpringPasswordEncoder : PasswordEncoder {
    private val bCryptPasswordEncoder = BCryptPasswordEncoder(10)

    override fun encode(rawPassword: String): String = bCryptPasswordEncoder.encode(rawPassword)

    override fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean = bCryptPasswordEncoder.matches(rawPassword, encodedPassword)
}
