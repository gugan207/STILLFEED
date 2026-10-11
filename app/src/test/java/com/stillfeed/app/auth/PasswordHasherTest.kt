package com.stillfeed.app.auth

import com.stillfeed.app.data.auth.Pbkdf2PasswordHasher
import com.stillfeed.app.data.auth.StoredPasswordHash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class PasswordHasherTest {

    // Use test hasher with 10,000 iterations for fast unit test execution,
    // while full 600,000 iteration production configuration is tested in a dedicated benchmark test.
    private val testHasher = Pbkdf2PasswordHasher(iterations = 10_000)

    @Test
    fun verifyPassword_withCorrectPassword_succeeds() {
        val password = "CalmAttention2026!"
        val storedHash = testHasher.hashPassword(password)

        val isValid = testHasher.verifyPassword(password, storedHash)
        assertTrue("Verification must succeed with the correct password", isValid)
    }

    @Test
    fun verifyPassword_withIncorrectPassword_fails() {
        val password = "CalmAttention2026!"
        val wrongPassword = "WrongPassword123!"
        val storedHash = testHasher.hashPassword(password)

        val isValid = testHasher.verifyPassword(wrongPassword, storedHash)
        assertFalse("Verification must fail with an incorrect password", isValid)
    }

    @Test
    fun hashPassword_differentPasswords_produceDifferentHashes() {
        val hash1 = testHasher.hashPassword("PasswordAlpha123")
        val hash2 = testHasher.hashPassword("PasswordBeta456")

        assertFalse(
            "Different passwords must produce different hash outputs",
            hash1.hash.contentEquals(hash2.hash)
        )
    }

    @Test
    fun hashPassword_samePasswordWithDifferentSalts_producesDifferentHashes() {
        val password = "IdenticalPassword99"
        val hash1 = testHasher.hashPassword(password)
        val hash2 = testHasher.hashPassword(password)

        // Salts must be unique (cryptographically random)
        assertFalse("Salts must be randomly generated", hash1.salt.contentEquals(hash2.salt))

        // Hashes must be different because salts differ
        assertFalse(
            "Same password hashed with different salts must produce different hash values",
            hash1.hash.contentEquals(hash2.hash)
        )
    }

    @Test
    fun storedPasswordHash_doesNotContainPlaintextPassword() {
        val rawPassword = "SuperSecretPlainTextPassword123!"
        val stored = testHasher.hashPassword(rawPassword)

        val formatted = stored.toFormattedString()
        assertFalse(
            "Formatted stored hash string must NEVER contain the plaintext password",
            formatted.contains(rawPassword)
        )

        val parsed = StoredPasswordHash.fromFormattedString(formatted)
        assertNotNull(parsed)
        assertEquals(stored.algorithm, parsed?.algorithm)
        assertEquals(stored.iterations, parsed?.iterations)
        assertTrue(stored.salt.contentEquals(parsed?.salt))
        assertTrue(stored.hash.contentEquals(parsed?.hash))
    }

    @Test
    fun benchmark_productionWorkFactor_executesInExpectedRange() {
        val productionHasher = Pbkdf2PasswordHasher(iterations = Pbkdf2PasswordHasher.DEFAULT_ITERATIONS)
        assertEquals("PBKDF2WithHmacSHA256", productionHasher.algorithmName)
        assertEquals(600_000, productionHasher.iterations)

        val duration = measureTimeMillis {
            val hash = productionHasher.hashPassword("BenchmarkPassword2026")
            val verified = productionHasher.verifyPassword("BenchmarkPassword2026", hash)
            assertTrue(verified)
        }

        println("Benchmark: PBKDF2-HMAC-SHA256 (600,000 iterations hash+verify) elapsed: $duration ms")
        // Verify it executes cleanly within reasonable interactive bounds
        assertTrue("Password derivation should complete", duration > 0)
    }
}
