package com.securevault.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class KeyStoreManager(val storageConfig: VaultStorageConfig) {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private val keyLock = Any()
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun getOrCreateSecretKey(): SecretKey = synchronized(keyLock) {
        try {
            getExistingKey() ?: createNewKey()
        } catch (e: Exception) {
            throw KeyRetrievalException(e)
        }
    }

    private fun getExistingKey(): SecretKey? {
        val entry = keyStore.getEntry(storageConfig.keyAlias, null) ?: return null
        check(entry is KeyStore.SecretKeyEntry) { "Vault alias is occupied by a non-secret-key entry" }
        return entry.secretKey
    }

    private fun createNewKey(): SecretKey {
        try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec =
                KeyGenParameterSpec.Builder(storageConfig.keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,)
                     .setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false)
                    .build()
            // Key material is generated inside Android Keystore, not from application random bytes.
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        } catch (e: Exception) {
            throw KeyGenerationException(e)
        }
    }

    fun deleteKey() = synchronized(keyLock) {
        keyStore.deleteEntry(storageConfig.keyAlias)
    }
}
