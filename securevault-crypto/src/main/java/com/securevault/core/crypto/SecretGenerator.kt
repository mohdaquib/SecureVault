package com.securevault.core.crypto

import java.security.SecureRandom

/** Central source for application-generated secret bytes. Never seed with predictable values. */
internal class SecretGenerator(private val random: SecureRandom = SecureRandom()) {
    fun generatePassphrase(): ByteArray = generateBytes(PASSPHRASE_BYTES)

    /** Each request receives a fresh buffer filled by the cryptographic random source. */
    fun generateBytes(byteCount: Int): ByteArray {
        require(byteCount > 0) { "Secret length must be positive" }
        return ByteArray(byteCount).also(random::nextBytes)
    }

    companion object {
        const val PASSPHRASE_BYTES: Int = 32
        val Default: SecretGenerator = SecretGenerator()
    }
}
