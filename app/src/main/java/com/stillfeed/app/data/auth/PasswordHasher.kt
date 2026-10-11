package com.stillfeed.app.data.auth

/**
 * Encapsulates a cryptographically derived password hash along with its verification parameters.
 *
 * Security rules:
 * - NEVER contains the plaintext password.
 * - Stores algorithm name, iteration work factor, random salt bytes, and derived hash bytes.
 */
data class StoredPasswordHash(
    val algorithm: String,
    val iterations: Int,
    val salt: ByteArray,
    val hash: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StoredPasswordHash
        if (algorithm != other.algorithm) return false
        if (iterations != other.iterations) return false
        if (!salt.contentEquals(other.salt)) return false
        if (!hash.contentEquals(other.hash)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = algorithm.hashCode()
        result = 31 * result + iterations
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + hash.contentHashCode()
        return result
    }

    /**
     * Serializes parameters into standard format: algorithm:iterations:saltHex:hashHex
     */
    fun toFormattedString(): String {
        val saltHex = salt.joinToString("") { "%02x".format(it) }
        val hashHex = hash.joinToString("") { "%02x".format(it) }
        return "$algorithm:$iterations:$saltHex:$hashHex"
    }

    companion object {
        fun fromFormattedString(formatted: String): StoredPasswordHash? {
            val parts = formatted.split(":")
            if (parts.size != 4) return null
            val algorithm = parts[0]
            val iterations = parts[1].toIntOrNull() ?: return null
            val salt = parts[2].chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val hash = parts[3].chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            return StoredPasswordHash(algorithm, iterations, salt, hash)
        }
    }
}

/**
 * Dedicated password-hashing abstraction.
 *
 * Specifically designed for slow, computationally intensive password key derivation.
 * Keeps authentication repositories independent of concrete cryptographic implementations
 * and prepared for production cloud authentication providers.
 */
interface PasswordHasher {
    val algorithmName: String
    val iterations: Int

    /**
     * Derives a cryptographic hash from the candidate password using a secure random salt.
     */
    fun hashPassword(password: CharSequence): StoredPasswordHash

    /**
     * Verifies whether [password] matches [storedHash] using timing-safe comparison.
     */
    fun verifyPassword(password: CharSequence, storedHash: StoredPasswordHash): Boolean
}
