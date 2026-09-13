package com.securevault.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class KeyStoreManager {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "securevault_db_key"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun getOrCreateSecretKey(): SecretKey =
        try {
            getExistingKey() ?: createNewKey()
        } catch (e: Exception) {
            throw KeyRetrievalException(e)
        }

    private fun getExistingKey(): SecretKey? {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey
    }

    private fun createNewKey(): SecretKey {
        try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec =
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,)
                     .setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false)
                    .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        } catch (e: Exception) {
            throw KeyGenerationException(e)
        }
    }

    fun deleteKey() {
        keyStore.deleteEntry(KEY_ALIAS)
    }
}
