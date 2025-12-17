package com.github.butvinmitmo.userservice.util

import com.github.butvinmitmo.userservice.config.SecurityConfig
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.io.File

class PasswordHashGenerator {
    private val passwordEncoder: PasswordEncoder = SecurityConfig().passwordEncoder()

    @Test
    fun `generate BCrypt hash for Admin@123`() {
        val password = "Admin@123"
        val hash = passwordEncoder.encode(password)

        val output =
            buildString {
                appendLine("=".repeat(80))
                appendLine("Password: $password")
                appendLine("BCrypt Hash: $hash")
                appendLine("=".repeat(80))
                appendLine()
                appendLine("Verification test:")
                appendLine("Hash matches password: ${passwordEncoder.matches(password, hash)}")
                appendLine("=".repeat(80))
            }

        println(output)

        // Write to file for easy access
        File("/tmp/bcrypt-hash.txt").writeText(output)
        File("/tmp/hash-only.txt").writeText(hash)

        // Verify the hash works
        assert(passwordEncoder.matches(password, hash)) {
            "Generated hash does not match the password!"
        }
    }
}
