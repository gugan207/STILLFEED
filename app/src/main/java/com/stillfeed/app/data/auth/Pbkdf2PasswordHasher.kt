package com.stillfeed.app.data.auth

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Modern PBKDF2-HMAC-SHA256 Password Hasher.
 *
 * Implements OWASP Password Storage Guidelines:
 * - Key derivation algorithm: PBKDF2WithHmacSHA256
 * - Work factor: 600,000 iterations (slow key derivation preventing GPU cracking)
 * - Cryptographically secure random salt: 16 bytes (128 bits) via [SecureRandom]
 * - Derived key length: 256 bits (32 bytes)
 * - Timing-safe comparison: [MessageDigest.isEqual] to prevent timing attacks
 * - Immediate memory wiping of sensitive character arrays
 */
class Pbkdf2PasswordHasher(
    override val iterations: Int = DEFAULT_ITERATIONS,
    private val keyLengthBits: Int = DEFAULT_KEY_LENGTH_BITS,
    private val saltLengthBytes: Int = DEFAULT_SALT_LENGTH_BYTES
) : PasswordHasher {

    override val algorithmName: String = ALGORITHM_NAME

    private val secureRandom = SecureRandom()
    private val secretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM_NAME)

    override fun hashPassword(password: CharSequence): StoredPasswordHash {
        val salt = ByteArray(saltLengthBytes)
        secureRandom.nextBytes(salt)

        val hash = deriveKey(password, salt, iterations, keyLengthBits)
        return StoredPasswordHash(
            algorithm = algorithmName,
            iterations = iterations,
            salt = salt,
            hash = hash
        )
    }

    override fun verifyPassword(password: CharSequence, storedHash: StoredPasswordHash): Boolean {
        if (storedHash.algorithm != algorithmName) {
            return false
        }

        val candidateHash = deriveKey(
            password = password,
            salt = storedHash.salt,
            iterations = storedHash.iterations,
            keyLengthBits = storedHash.hash.size * 8
        )

        // Timing-safe comparison prevents side-channel timing attacks
        return MessageDigest.isEqual(candidateHash, storedHash.hash)
    }

    private fun deriveKey(
        password: CharSequence,
        salt: ByteArray,
        iterations: Int,
        keyLengthBits: Int
    ): ByteArray {
        val charArray = CharArray(password.length)
        for (i in password.indices) {
            charArray[i] = password[i]
        }

        return try {
            val spec = PBEKeySpec(charArray, salt, iterations, keyLengthBits)
            secretKeyFactory.generateSecret(spec).encoded
        } finally {
            // Securely wipe password character array from memory immediately
            charArray.fill('\u0000')
        }
    }

    companion object {
        const val ALGORITHM_NAME = "PBKDF2WithHmacSHA256"
        const val DEFAULT_ITERATIONS = 600_000
        const val DEFAULT_KEY_LENGTH_BITS = 256
        const val DEFAULT_SALT_LENGTH_BYTES = 16
    }
}
