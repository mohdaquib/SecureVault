package com.securevault.demo.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.securevault.core.crypto.KeyStoreManager
import com.securevault.core.crypto.SecurePassphraseStore
import com.securevault.core.crypto.VaultStorageConfig
import com.securevault.demo.data.local.NoteEntity
import com.securevault.demo.data.local.SecureDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

@RunWith(AndroidJUnit4::class)
class VaultStorageMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private inline fun <T> SecureDatabase.withDatabase(block: (SecureDatabase) -> T): T =
        try { block(this) } finally { close() }

    @Test fun originalDemoDatabaseReopensAndNewVaultRemainsIndependent() = runBlocking {
        System.loadLibrary("sqlcipher")
        val legacy = VaultStorageConfig.legacyDemo()
        val fresh = VaultStorageConfig("migration-new")
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        fun clear(config: VaultStorageConfig) {
            context.deleteDatabase(config.databaseName)
            context.getSharedPreferences(config.preferencesName, 0).edit().clear().commit()
            keyStore.deleteEntry(config.keyAlias)
        }
        clear(legacy)
        clear(fresh)
        try {
            // Fixture uses literal pre-namespace locations and the original encryption format.
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generator.init(KeyGenParameterSpec.Builder("securevault_db_key",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            val oldKey = generator.generateKey()
            val passphrase = ByteArray(32) { (it + 1).toByte() }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, oldKey)
            val preferences = context.getSharedPreferences("securevault_crypto", 0)
            preferences.edit()
                .putString("encrypted_passphrase", Base64.encodeToString(cipher.doFinal(passphrase), Base64.NO_WRAP))
                .putString("passphrase_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP)).commit()
            val before = preferences.all
            val note = NoteEntity("legacy-note", "Existing title", "Existing content", Instant.EPOCH, Instant.EPOCH)
            Room.databaseBuilder(context, SecureDatabase::class.java, "securevault.db")
                .openHelperFactory(SupportOpenHelperFactory(passphrase.copyOf())).build().withDatabase { db ->
                    db.noteDao().insert(note)
                }

            val legacyStore = SecurePassphraseStore(context, KeyStoreManager(legacy))
            assertArrayEquals(passphrase, legacyStore.getOrCreatePassphrase())
            SecureDatabase.create(context, legacyStore).withDatabase { db ->
                assertEquals(listOf(note), db.noteDao().observeNotes().first())
            }
            val freshStore = SecurePassphraseStore(context, KeyStoreManager(fresh))
            assertFalse(passphrase.contentEquals(freshStore.getOrCreatePassphrase()))
            SecureDatabase.create(context, freshStore).withDatabase { db ->
                assertTrue(db.noteDao().observeNotes().first().isEmpty())
                db.noteDao().insert(note.copy(id = "new-note"))
            }
            assertTrue(keyStore.containsAlias(legacy.keyAlias))
            assertTrue(keyStore.containsAlias(fresh.keyAlias))
            assertEquals(before, preferences.all)
            clear(fresh)
            SecureDatabase.create(context, legacyStore).withDatabase { db ->
                assertEquals(listOf(note), db.noteDao().observeNotes().first())
            }
        } finally {
            clear(fresh)
            clear(legacy)
        }
    }
}

