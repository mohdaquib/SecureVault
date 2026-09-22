package com.securevault.core.crypto

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import java.util.concurrent.Executors
import java.util.concurrent.Callable

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class SecurePassphraseStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun key() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test fun passphraseIsGeneratedOnlyOnFirstOpen() {
        val config = VaultStorageConfig("generation-once")
        val secretKey = key()
        val source = RecordingSecureRandom()
        val generator = SecretGenerator(source)
        val first = SecurePassphraseStore(context, config, generator) { secretKey }.getOrCreatePassphrase()
        val reopened = SecurePassphraseStore(context, config, generator) { secretKey }.getOrCreatePassphrase()

        assertEquals(32, first.size)
        assertArrayEquals(ByteArray(32) { 0x5a.toByte() }, first)
        assertArrayEquals(first, reopened)
        assertEquals(listOf(32), source.requestedSizes)
    }

    @Test fun generationFailureLeavesPreferencesUntouched() {
        val config = VaultStorageConfig("generation-failure")
        val failure = IllegalStateException("Random source unavailable")
        val source = object : java.security.SecureRandom() {
            override fun nextBytes(bytes: ByteArray) { throw failure }
        }
        val store = SecurePassphraseStore(context, config, SecretGenerator(source)) {
            error("Must not request a key after generation failed")
        }
        assertSame(failure, assertThrows(IllegalStateException::class.java) { store.getOrCreatePassphrase() })
        assertTrue(context.getSharedPreferences(config.preferencesName, 0).all.isEmpty())
    }

    @Test fun distinctVaultsAreIsolatedAndSameNamespaceReopens() {
        val a = VaultStorageConfig("test-a")
        val b = VaultStorageConfig("test-b")
        val keyA = key()
        val keyB = key()
        val first = SecurePassphraseStore(context, a, SecretGenerator(RecordingSecureRandom())) { keyA }.getOrCreatePassphrase()
        val second = SecurePassphraseStore(context, b, SecretGenerator(RecordingSecureRandom(0x6b.toByte()))) { keyB }.getOrCreatePassphrase()
        assertFalse(first.contentEquals(second))
        assertArrayEquals(first, SecurePassphraseStore(context, a, SecretGenerator(RecordingSecureRandom())) { keyA }.getOrCreatePassphrase())
        context.getSharedPreferences(a.preferencesName, 0).edit().clear().commit()
        assertArrayEquals(second, SecurePassphraseStore(context, b, SecretGenerator(RecordingSecureRandom(0x6b.toByte()))) { keyB }.getOrCreatePassphrase())
    }

    @Test fun legacyCiphertextWrittenByOriginalFormatIsReusedWithoutRewriting() {
        val oldKey = key()
        val originalPassphrase = ByteArray(32) { it.toByte() }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, oldKey)
        // Deliberately hard-coded old format, independent of the new naming implementation.
        val prefs = context.getSharedPreferences("securevault_crypto", 0)
        prefs.edit().putString("encrypted_passphrase", Base64.encodeToString(cipher.doFinal(originalPassphrase), Base64.NO_WRAP))
            .putString("passphrase_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP)).commit()
        val before = prefs.all
        val migrated = SecurePassphraseStore(context, VaultStorageConfig.legacyDemo()) { oldKey }
        assertArrayEquals(originalPassphrase, migrated.getOrCreatePassphrase())
        assertEquals(before, prefs.all)
        val fresh = SecurePassphraseStore(context, VaultStorageConfig("demo"),
            SecretGenerator(RecordingSecureRandom())) { key() }
        assertFalse(originalPassphrase.contentEquals(fresh.getOrCreatePassphrase()))
        assertEquals(before, prefs.all)
    }

    @Test fun wrongKeyFailsWithoutReplacingLegacyCiphertext() {
        val config = VaultStorageConfig.legacyDemo()
        val correctKey = key()
        SecurePassphraseStore(context, config) { correctKey }.getOrCreatePassphrase()
        val prefs = context.getSharedPreferences(config.preferencesName, 0)
        val before = prefs.all
        val wrongKey = key()
        assertThrows(java.security.GeneralSecurityException::class.java) {
            SecurePassphraseStore(context, config) { wrongKey }.getOrCreatePassphrase()
        }
        assertEquals(before, prefs.all)
    }

    @Test fun partialStateFailsWithoutOverwriting() {
        val config = VaultStorageConfig("partial")
        val prefs = context.getSharedPreferences(config.preferencesName, 0)
        prefs.edit().putString("encrypted_passphrase", "existing").commit()
        val store = SecurePassphraseStore(context, config) { error("Must not create a key") }
        assertThrows(IllegalStateException::class.java) { store.getOrCreatePassphrase() }
        assertEquals("existing", prefs.getString("encrypted_passphrase", null))
    }

    @Test fun concurrentInstancesReturnTheSamePassphrase() {
        val config = VaultStorageConfig("concurrent")
        val secretKey = key()
        val executor = Executors.newFixedThreadPool(4)
        try {
            val results = executor.invokeAll((1..12).map {
                Callable { SecurePassphraseStore(context, config) { secretKey }.getOrCreatePassphrase() }
            }).map { it.get() }
            results.forEach { assertArrayEquals(results.first(), it) }
        } finally {
            executor.shutdownNow()
        }
    }
}
