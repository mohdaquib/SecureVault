package com.securevault.core.crypto

import android.content.Context
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurePassphraseStore(private val context: Context,
    private val keyStoreManager: KeyStoreManager) {
    private val prefs = context.getSharedPreferences("securevault_crypto",
        Context.MODE_PRIVATE)

    fun getOrCreatePassphrase(): ByteArray {
        val encrypted = prefs.getString("encrypted_passphrase", null)
        val iv = prefs.getString("passphrase_iv", null)

        return if (encrypted != null && iv != null) {
            decrypt(encrypted, iv)
        } else {
            val passphrase = kotlin.random.Random.nextBytes(32)
            val (cipherText, ivBytes) = encrypt(passphrase)
            prefs.edit()
                .putString("encrypted_passphrase", cipherText)
                .putString("passphrase_iv", ivBytes)
                .apply()
            passphrase
        }
    }

    private fun encrypt(data: ByteArray): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key: SecretKey = keyStoreManager.getOrCreateSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val encrypted = cipher.doFinal(data)
        return Base64.encodeToString(encrypted, Base64.NO_WRAP) to
                Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
    }

    private fun decrypt(encrypted: String, iv: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = keyStoreManager.getOrCreateSecretKey()
        cipher.init(Cipher.DECRYPT_MODE, key,
            GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
        )
        return cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP))
    }
}