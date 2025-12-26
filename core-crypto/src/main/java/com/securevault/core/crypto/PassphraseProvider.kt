package com.securevault.core.crypto

import javax.crypto.SecretKey

class PassphraseProvider(
    private val keyStoreManager: KeyStoreManager,
) {
    fun getPassphrase(): ByteArray {
        val secretKey: SecretKey = keyStoreManager.getOrCreateSecretKey()
        return secretKey.encoded ?: throw KeyRetrievalException(
            IllegalStateException("SecretKey encoding unavailable"),
        )
    }
}
