package dev.rockyj.example.demo.services

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.experimental.xor

/**
 * Modern secure password hashing service using Argon2id
 * Falls back to PBKDF2 with SHA-512 if Argon2 is not available
 */
@Service
class PasswordService {
    companion object {
        // Argon2id parameters (OWASP recommended for 2024)
        private const val ARGON2_SALT_LENGTH = 16
        private const val ARGON2_HASH_LENGTH = 32
        private const val ARGON2_ITERATIONS = 3 // Number of iterations
        private const val ARGON2_MEMORY = 65536 // Memory usage in KiB (64 MB)
        private const val ARGON2_PARALLELISM = 4 // Number of threads

        // PBKDF2 parameters (fallback)
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA512"
        private const val PBKDF2_ITERATIONS = 600000 // OWASP recommendation for 2024
        private const val PBKDF2_SALT_LENGTH = 32
        private const val PBKDF2_HASH_LENGTH = 64

        // Hash format prefixes
        private const val ARGON2_PREFIX = "\$argon2id$"
        private const val PBKDF2_PREFIX = "\$pbkdf2-sha512$"
    }

    private val secureRandom = SecureRandom()

    /**
     * Hash a password using Argon2id (preferred) or PBKDF2-SHA512 (fallback)
     */
    fun hashPassword(password: String): String =
        try {
            hashWithArgon2(password)
        } catch (e: Exception) {
            // Fall back to PBKDF2 if Argon2 is not available
            hashWithPBKDF2(password)
        }

    /**
     * Verify a password against its hash
     */
    fun verifyPassword(
        password: String,
        hash: String,
    ): Boolean =
        when {
            hash.startsWith(ARGON2_PREFIX) -> verifyArgon2(password, hash)
            hash.startsWith(PBKDF2_PREFIX) -> verifyPBKDF2(password, hash)
            else -> throw IllegalArgumentException("Unknown hash format")
        }

    /**
     * Check if a password hash needs to be upgraded (rehashed with better parameters)
     */
    fun needsRehash(hash: String): Boolean =
        when {
            hash.startsWith(ARGON2_PREFIX) -> checkArgon2NeedsRehash(hash)
            hash.startsWith(PBKDF2_PREFIX) -> true // Always upgrade PBKDF2 to Argon2
            else -> true
        }

    /**
     * Hash password using Argon2id
     */
    private fun hashWithArgon2(password: String): String {
        val salt = ByteArray(ARGON2_SALT_LENGTH)
        secureRandom.nextBytes(salt)

        val params =
            Argon2Parameters
                .Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(ARGON2_ITERATIONS)
                .withMemoryAsKB(ARGON2_MEMORY)
                .withParallelism(ARGON2_PARALLELISM)
                .withSalt(salt)
                .build()

        val generator = Argon2BytesGenerator()
        generator.init(params)

        val hash = ByteArray(ARGON2_HASH_LENGTH)
        generator.generateBytes(password.toByteArray(Charsets.UTF_8), hash)

        // Format: $argon2id$v=19$m=65536,t=3,p=4$salt$hash
        val saltBase64 = Base64.getEncoder().withoutPadding().encodeToString(salt)
        val hashBase64 = Base64.getEncoder().withoutPadding().encodeToString(hash)

        return ARGON2_PREFIX +
                "v=${Argon2Parameters.ARGON2_VERSION_13}\$" +
                "m=$ARGON2_MEMORY,t=$ARGON2_ITERATIONS,p=$ARGON2_PARALLELISM\$" +
                "$saltBase64\$" +
                hashBase64
    }

    /**
     * Verify password using Argon2id
     */
    private fun verifyArgon2(
        password: String,
        hash: String,
    ): Boolean {
        try {
            val parts = hash.split('$')
            if (parts.size != 6) return false

            // Parse parameters
            val version = parts[2].substring(2).toInt()
            val paramParts = parts[3].split(',')
            val memory = paramParts[0].substring(2).toInt()
            val iterations = paramParts[1].substring(2).toInt()
            val parallelism = paramParts[2].substring(2).toInt()

            val salt = Base64.getDecoder().decode(parts[4])
            val expectedHash = Base64.getDecoder().decode(parts[5])

            val params =
                Argon2Parameters
                    .Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(version)
                    .withIterations(iterations)
                    .withMemoryAsKB(memory)
                    .withParallelism(parallelism)
                    .withSalt(salt)
                    .build()

            val generator = Argon2BytesGenerator()
            generator.init(params)

            val actualHash = ByteArray(expectedHash.size)
            generator.generateBytes(password.toByteArray(Charsets.UTF_8), actualHash)

            return constantTimeEquals(expectedHash, actualHash)
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Check if Argon2 hash needs rehashing
     */
    private fun checkArgon2NeedsRehash(hash: String): Boolean {
        try {
            val parts = hash.split('$')
            if (parts.size != 6) return true

            val paramParts = parts[3].split(',')
            val memory = paramParts[0].substring(2).toInt()
            val iterations = paramParts[1].substring(2).toInt()
            val parallelism = paramParts[2].substring(2).toInt()

            return memory < ARGON2_MEMORY ||
                    iterations < ARGON2_ITERATIONS ||
                    parallelism < ARGON2_PARALLELISM
        } catch (e: Exception) {
            return true
        }
    }

    /**
     * Hash password using PBKDF2 with SHA-512 (fallback)
     */
    private fun hashWithPBKDF2(password: String): String {
        val salt = ByteArray(PBKDF2_SALT_LENGTH)
        secureRandom.nextBytes(salt)

        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_HASH_LENGTH * 8)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val hash = factory.generateSecret(spec).encoded

        spec.clearPassword()

        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val hashBase64 = Base64.getEncoder().encodeToString(hash)

        return "$PBKDF2_PREFIX$PBKDF2_ITERATIONS\$$saltBase64\$$hashBase64"
    }

    /**
     * Verify password using PBKDF2
     */
    private fun verifyPBKDF2(
        password: String,
        hash: String,
    ): Boolean {
        try {
            val parts = hash.split('$')
            if (parts.size != 5) return false

            val iterations = parts[2].toInt()
            val salt = Base64.getDecoder().decode(parts[3])
            val expectedHash = Base64.getDecoder().decode(parts[4])

            val spec = PBEKeySpec(password.toCharArray(), salt, iterations, expectedHash.size * 8)
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            val actualHash = factory.generateSecret(spec).encoded

            spec.clearPassword()

            return constantTimeEquals(expectedHash, actualHash)
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Constant-time comparison to prevent timing attacks
     */
    private fun constantTimeEquals(
        a: ByteArray,
        b: ByteArray,
    ): Boolean {
        if (a.size != b.size) return false

        var result: Byte = 0
        for (i in a.indices) {
            result = result xor (a[i] xor b[i])
        }
        return result.toInt() == 0
    }
}
