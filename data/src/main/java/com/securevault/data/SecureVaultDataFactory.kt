package com.securevault.data

import android.content.Context
import com.securevault.core.crypto.KeyStoreManager
import com.securevault.core.crypto.PassphraseProvider
import com.securevault.data.local.SecureDatabase
import com.securevault.data.repository.NotesRepositoryImpl
import com.securevault.domain.repository.NotesRepository

object SecureVaultDataFactory {
    fun createNotesRepository(context: Context): NotesRepository {
        val keyStoreManager = KeyStoreManager()
        val passphraseProvider = PassphraseProvider(keyStoreManager)
        val db =
            SecureDatabase.create(
                context = context.applicationContext,
                passphrase = passphraseProvider.getPassphrase(),
            )
        return NotesRepositoryImpl(db.noteDao())
    }
}
