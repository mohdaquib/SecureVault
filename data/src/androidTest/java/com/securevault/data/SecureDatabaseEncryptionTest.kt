package com.securevault.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.securevault.core.crypto.KeyStoreManager
import com.securevault.core.crypto.SecurePassphraseStore
import com.securevault.data.local.NoteEntity
import com.securevault.data.local.SecureDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class SecureDatabaseEncryptionTest {

    @Before
    fun setup() {
        System.loadLibrary("sqlcipher")
    }

    @Test
    fun database_is_encrypted_at_rest() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val keyStoreManager = KeyStoreManager()
        val passphraseStore = SecurePassphraseStore(context, keyStoreManager)

        val db = SecureDatabase.create(context = context, passphraseStore = passphraseStore)
        val note = NoteEntity(
            id = "test-id",
            title = "SECRET_TITLE",
            content = "VERY_SECRET_CONTENT",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        db.noteDao().insert(note)
        db.close()

        val dbFile: File = context.getDatabasePath("securevault.db")
        val rawBytes = dbFile.readBytes()
        val rawText = String(rawBytes)

        assertFalse(rawText.contains("SECRET_TITLE"))
        assertFalse(rawText.contains("VERY_SECRET_CONTENT"))
    }
}