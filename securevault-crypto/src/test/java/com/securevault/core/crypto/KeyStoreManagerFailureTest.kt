package com.securevault.core.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.securevault.sdk.SecureVaultCryptoException
import com.securevault.sdk.SecureVaultCryptoFailure.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.ProviderException
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class KeyStoreManagerFailureTest {
    private class Backend : KeyStoreBackend {
        var stored: StoredKey? = null
        var failure: Exception? = null
        var creates = 0
        override fun find(): StoredKey? { failure?.let { throw it }; return stored }
        override fun create(): StoredKey {
            creates++
            return StoredKey(SecretKeySpec(ByteArray(32), "AES")).also { stored = it }
        }
        override fun delete() { stored = null }
    }

    @Test fun readOnlyMissingKeyDoesNotCreate() {
        val backend = Backend()
        val manager = KeyStoreManager(VaultStorageConfig("missing"), backend)
        assertEquals(MISSING_KEY, assertThrows(SecureVaultCryptoException::class.java) { manager.getSecretKey() }.failure)
        assertEquals(0, backend.creates)
    }

    @Test fun unknownLookupErrorNeverCreatesOrReplaces() {
        val backend = Backend()
        val manager = KeyStoreManager(VaultStorageConfig("failure"), backend)
        val original = manager.getOrCreateSecretKey()
        backend.failure = ProviderException("key material must not escape")
        assertEquals(UNEXPECTED_PROVIDER_FAILURE,
            assertThrows(SecureVaultCryptoException::class.java) { manager.getOrCreateSecretKey() }.failure)
        assertEquals(1, backend.creates)
        assertSame(original, backend.stored!!.key)
        backend.stored = null
        assertThrows(SecureVaultCryptoException::class.java) { manager.getOrCreateSecretKey() }
        assertEquals(1, backend.creates)
    }

    @Test fun productionStoreWiringDoesNotRecreateALostKey() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val backend = Backend()
        val config = VaultStorageConfig("lost-key-store")
        val manager = KeyStoreManager(config, backend)
        SecurePassphraseStore(context, manager).getOrCreatePassphrase()
        val prefs = context.getSharedPreferences(config.preferencesName, 0)
        val before = prefs.all
        backend.stored = null
        assertEquals(MISSING_KEY, assertThrows(SecureVaultCryptoException::class.java) {
            SecurePassphraseStore(context, manager).getOrCreatePassphrase()
        }.failure)
        assertEquals(1, backend.creates)
        assertEquals(before, prefs.all)
        backend.failure = ProviderException("do not expose this")
        val error = assertThrows(SecureVaultCryptoException::class.java) {
            SecurePassphraseStore(context, manager).getOrCreatePassphrase()
        }
        assertEquals(UNEXPECTED_PROVIDER_FAILURE, error.failure)
        assertNull(error.cause)
        assertEquals(1, backend.creates)
        assertEquals(before, prefs.all)
    }

    @Test fun creationRequiresConfirmedAbsenceAndExistingKeysAreReused() {
        val backend = Backend()
        val manager = KeyStoreManager(VaultStorageConfig("new"), backend)
        val created = manager.getOrCreateSecretKey()
        assertSame(created, manager.getOrCreateSecretKey())
        assertSame(created, manager.getSecretKey())
        assertEquals(1, backend.creates)
    }
}
