package com.securevault.core.crypto

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurePassphraseStore internal constructor(
    context: Context,
    val storageConfig: VaultStorageConfig,
    private val getKey: () -> SecretKey,
) {
    constructor(context: Context, keyStoreManager: KeyStoreManager) :
        this(context, keyStoreManager.storageConfig, keyStoreManager::getOrCreateSecretKey)

    private val prefs = context.applicationContext.getSharedPreferences(
        storageConfig.preferencesName, Context.MODE_PRIVATE,
    )

    fun getOrCreatePassphrase(): ByteArray = synchronized(passphraseLock) {
        val encrypted = prefs.getString("encrypted_passphrase", null)
        val iv = prefs.getString("passphrase_iv", null)
        check((encrypted == null) == (iv == null)) { "Incomplete vault passphrase state" }
        if (encrypted != null && iv != null) {
            decrypt(encrypted, iv)
        } else {
            val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val (cipherText, ivBytes) = encrypt(passphrase)
            check(prefs.edit()
                .putString("encrypted_passphrase", cipherText)
                .putString("passphrase_iv", ivBytes)
                .commit()) { "Could not persist vault passphrase" }
            passphrase
        }
    }

    private fun encrypt(data: ByteArray): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        return Base64.encodeToString(cipher.doFinal(data), Base64.NO_WRAP) to
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
    }

    private fun decrypt(encrypted: String, iv: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getKey(),
            GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        return cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP))
    }

    private companion object {
        // Serializes initial creation across all instances in this process.
        val passphraseLock = Any()
    }
}
