package com.securevault.core.crypto

import android.content.Context
import android.util.Base64
import com.securevault.sdk.SecureVaultCryptoException
import com.securevault.sdk.SecureVaultCryptoFailure
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurePassphraseStore internal constructor(
    context: Context,
    val storageConfig: VaultStorageConfig,
    private val secretGenerator: SecretGenerator = SecretGenerator.Default,
    private val createKey: (() -> SecretKey)? = null,
    private val authentication: AuthenticationEvidence = AuthenticationEvidence(),
    private val getKey: () -> SecretKey,
) {
    constructor(context: Context, keyStoreManager: KeyStoreManager) : this(
        context, keyStoreManager.storageConfig,
        createKey = keyStoreManager::getOrCreateSecretKey,
        authentication = keyStoreManager.authentication,
        getKey = keyStoreManager::getSecretKey,
    )

    private val appContext = context.applicationContext
    private val prefs = cryptoOperation(CryptoOperation.STORAGE) {
        appContext.getSharedPreferences(storageConfig.preferencesName, Context.MODE_PRIVATE)
    }

    fun getOrCreatePassphrase(): ByteArray = synchronized(passphraseLock) {
        cryptoOperation(CryptoOperation.STORAGE, authentication) {
            val encrypted = storedString("encrypted_passphrase")
            val iv = storedString("passphrase_iv")
            if ((encrypted == null) != (iv == null)) corrupt()
            if (encrypted != null && iv != null) {
                decrypt(encrypted, iv)
            } else {
                // Lost preferences must not turn an existing database into a fresh vault.
                if (appContext.getDatabasePath(storageConfig.databaseName).exists()) corrupt()
                val passphrase = secretGenerator.generatePassphrase()
                try {
                    val (cipherText, ivBytes) = encrypt(passphrase)
                    if (!prefs.edit().putString("encrypted_passphrase", cipherText)
                            .putString("passphrase_iv", ivBytes).commit()) {
                        throw SecureVaultCryptoException(SecureVaultCryptoFailure.UNEXPECTED_PROVIDER_FAILURE)
                    }
                    passphrase
                } catch (error: Exception) {
                    passphrase.fill(0)
                    throw error
                }
            }
        }
    }

    private fun storedString(name: String): String? = try {
        prefs.getString(name, null)
    } catch (_: ClassCastException) { corrupt() }

    private fun encrypt(data: ByteArray): Pair<String, String> = cryptoOperation(CryptoOperation.ENCRYPT, authentication) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        // Only this new-storage path may request key creation. The provider generates the nonce.
        cipher.init(Cipher.ENCRYPT_MODE, createKey?.invoke() ?: getKey())
        val ciphertext = cipher.doFinal(data)
        authentication.recordSuccessfulUse()
        Base64.encodeToString(ciphertext, Base64.NO_WRAP) to Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
    }

    private fun decrypt(encrypted: String, iv: String): ByteArray = cryptoOperation(CryptoOperation.DECRYPT, authentication) {
        val ciphertext = decode(encrypted)
        val nonce = decode(iv)
        // Existing format: 32-byte passphrase, 16-byte authentication tag, 12-byte GCM nonce.
        if (ciphertext.size != SecretGenerator.PASSPHRASE_BYTES + 16 || nonce.size != 12) corrupt()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, nonce))
        val passphrase = cipher.doFinal(ciphertext)
        if (passphrase.size != SecretGenerator.PASSPHRASE_BYTES) {
            passphrase.fill(0)
            corrupt()
        }
        authentication.recordSuccessfulUse()
        passphrase
    }

    private fun decode(value: String): ByteArray = try {
        Base64.decode(value, Base64.NO_WRAP)
    } catch (_: IllegalArgumentException) { corrupt() }

    private fun corrupt(): Nothing = throw SecureVaultCryptoException(SecureVaultCryptoFailure.CORRUPT_CIPHERTEXT)

    private companion object { val passphraseLock = Any() }
}
