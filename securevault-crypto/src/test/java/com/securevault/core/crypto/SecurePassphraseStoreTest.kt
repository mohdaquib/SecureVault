package com.securevault.core.crypto

import com.securevault.sdk.SecureVaultCryptoException
import com.securevault.sdk.SecureVaultCryptoFailure
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
        assertEquals(SecureVaultCryptoFailure.UNEXPECTED_PROVIDER_FAILURE,
            assertThrows(SecureVaultCryptoException::class.java) { store.getOrCreatePassphrase() }.failure)
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
        assertEquals(SecureVaultCryptoFailure.CORRUPT_CIPHERTEXT, assertThrows(SecureVaultCryptoException::class.java) {
            SecurePassphraseStore(context, config) { wrongKey }.getOrCreatePassphrase()
        }.failure)
        assertEquals(before, prefs.all)
    }

    @Test fun partialStateFailsWithoutOverwriting() {
        val config = VaultStorageConfig("partial")
        val prefs = context.getSharedPreferences(config.preferencesName, 0)
        prefs.edit().putString("encrypted_passphrase", "existing").commit()
        val store = SecurePassphraseStore(context, config) { error("Must not create a key") }
        assertEquals(SecureVaultCryptoFailure.CORRUPT_CIPHERTEXT,
            assertThrows(SecureVaultCryptoException::class.java) { store.getOrCreatePassphrase() }.failure)
        assertEquals("existing", prefs.getString("encrypted_passphrase", null))
    }

    @Test fun existingCiphertextNeverUsesTheCreationPath() {
        val config = VaultStorageConfig("read-only-failure")
        val originalKey = key()
        SecurePassphraseStore(context, config) { originalKey }.getOrCreatePassphrase()
        val prefs = context.getSharedPreferences(config.preferencesName, 0)
        val before = prefs.all
        for (reason in SecureVaultCryptoFailure.entries) {
            var creates = 0
            val source = RecordingSecureRandom()
            val store = SecurePassphraseStore(context, config, SecretGenerator(source),
                createKey = { creates++; originalKey },
                getKey = { throw SecureVaultCryptoException(reason) })
            assertEquals(reason, assertThrows(SecureVaultCryptoException::class.java) {
                store.getOrCreatePassphrase()
            }.failure)
            assertEquals(0, creates)
            assertTrue(source.requestedSizes.isEmpty())
            assertEquals(before, prefs.all)
        }
    }

    @Test fun malformedRecordsFailBeforeKeyAccess() {
        val config = VaultStorageConfig("malformed")
        val prefs = context.getSharedPreferences(config.preferencesName, 0)
        for (record in listOf("%%%" to "%%%", "" to "", "AA==" to "AA==")) {
            prefs.edit().putString("encrypted_passphrase", record.first)
                .putString("passphrase_iv", record.second).commit()
            val before = prefs.all
            val store = SecurePassphraseStore(context, config) { error("Must not access key") }
            assertEquals(SecureVaultCryptoFailure.CORRUPT_CIPHERTEXT,
                assertThrows(SecureVaultCryptoException::class.java) { store.getOrCreatePassphrase() }.failure)
            assertEquals(before, prefs.all)
        }
        prefs.edit().putInt("encrypted_passphrase", 123).commit()
        assertEquals(SecureVaultCryptoFailure.CORRUPT_CIPHERTEXT,
            assertThrows(SecureVaultCryptoException::class.java) {
                SecurePassphraseStore(context, config) { error("Must not access key") }.getOrCreatePassphrase()
            }.failure)
    }

    @Test fun lostPreferencesForExistingDatabaseDoNotGenerateNewSecrets() {
        val config = VaultStorageConfig("lost-preferences")
        val database = context.getDatabasePath(config.databaseName)
        database.parentFile!!.mkdirs()
        database.writeBytes(byteArrayOf(1, 2, 3))
        try {
            val source = RecordingSecureRandom()
            val store = SecurePassphraseStore(context, config, SecretGenerator(source)) { error("Must not access key") }
            assertEquals(SecureVaultCryptoFailure.CORRUPT_CIPHERTEXT,
                assertThrows(SecureVaultCryptoException::class.java) { store.getOrCreatePassphrase() }.failure)
            assertTrue(source.requestedSizes.isEmpty())
            assertArrayEquals(byteArrayOf(1, 2, 3), database.readBytes())
        } finally { database.delete() }
    }

    @Test fun successfulTimedKeyUseThenAuthenticationRejectionReportsExpiry() {
        val config = VaultStorageConfig("timed-auth")
        val secretKey = key()
        val evidence = AuthenticationEvidence().apply { usesTimedAuthentication = true }
        SecurePassphraseStore(context, config, authentication = evidence) { secretKey }.getOrCreatePassphrase()
        val store = SecurePassphraseStore(context, config, authentication = evidence) {
            throw android.security.keystore.UserNotAuthenticatedException()
        }
        assertEquals(SecureVaultCryptoFailure.AUTHENTICATION_EXPIRED,
            assertThrows(SecureVaultCryptoException::class.java) { store.getOrCreatePassphrase() }.failure)
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
