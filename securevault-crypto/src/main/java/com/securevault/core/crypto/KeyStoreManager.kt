package com.securevault.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import com.securevault.sdk.SecureVaultCryptoException
import com.securevault.sdk.SecureVaultCryptoFailure
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory

internal data class StoredKey(val key: SecretKey, val usesTimedAuthentication: Boolean = false)

internal interface KeyStoreBackend {
    fun find(): StoredKey?
    fun create(): StoredKey
    fun delete()
}

class KeyStoreManager internal constructor(
    val storageConfig: VaultStorageConfig,
    private val backend: KeyStoreBackend,
) {
    constructor(storageConfig: VaultStorageConfig) : this(storageConfig, AndroidKeyStoreBackend(storageConfig.keyAlias))

    internal val authentication = AuthenticationEvidence()

    /** Read-only lookup. Never generates a replacement for lost or unusable key material. */
    fun getSecretKey(): SecretKey = synchronized(keyLock) {
        cryptoOperation(CryptoOperation.LOOKUP_KEY, authentication) {
            select(backend.find() ?: throw SecureVaultCryptoException(SecureVaultCryptoFailure.MISSING_KEY))
        }
    }

    /** Only for new storage: creation is permitted solely after a successful absent-entry lookup. */
    fun getOrCreateSecretKey(): SecretKey = synchronized(keyLock) {
        val existing = cryptoOperation(CryptoOperation.LOOKUP_KEY, authentication) { backend.find() }
        if (existing != null) select(existing) else {
            val created = cryptoOperation(CryptoOperation.CREATE_KEY, authentication) { backend.create() }
            authentication.reset()
            select(created)
        }
    }

    private fun select(stored: StoredKey): SecretKey {
        authentication.usesTimedAuthentication = stored.usesTimedAuthentication
        return stored.key
    }

    /** Explicit destructive operation; never invoked by recovery or error mapping. */
    fun deleteKey(): Unit = synchronized(keyLock) {
        cryptoOperation(CryptoOperation.DELETE_KEY, authentication) { backend.delete() }
        authentication.reset()
    }

    private companion object { val keyLock = Any() }
}

private class AndroidKeyStoreBackend(private val alias: String) : KeyStoreBackend {
    private val keyStore: KeyStore by lazy {
        cryptoOperation(CryptoOperation.LOAD_KEYSTORE) {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        }
    }

    override fun find(): StoredKey? {
        val entry = keyStore.getEntry(alias, null)
        if (entry == null) {
            if (keyStore.containsAlias(alias)) {
                throw SecureVaultCryptoException(SecureVaultCryptoFailure.UNEXPECTED_PROVIDER_FAILURE)
            }
            return null
        }
        if (entry !is KeyStore.SecretKeyEntry) {
            throw SecureVaultCryptoException(SecureVaultCryptoFailure.UNEXPECTED_PROVIDER_FAILURE)
        }
        val info = SecretKeyFactory.getInstance(entry.secretKey.algorithm, "AndroidKeyStore")
            .getKeySpec(entry.secretKey, KeyInfo::class.java) as KeyInfo
        return StoredKey(entry.secretKey, info.isUserAuthenticationRequired && info.userAuthenticationValidityDurationSeconds > 0)
    }

    override fun create(): StoredKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build()
        generator.init(spec)
        return StoredKey(generator.generateKey())
    }

    override fun delete() { keyStore.deleteEntry(alias) }
}
