package com.securevault.data

import android.content.Context
import com.securevault.core.crypto.KeyStoreManager
import com.securevault.core.crypto.SecurePassphraseStore
import com.securevault.data.local.SecureDatabase
import com.securevault.data.repository.NotesRepositoryImpl
import com.securevault.domain.repository.NotesRepository

object SecureVaultDataFactory {
    fun createNotesRepository(context: Context): NotesRepository {
        val keyStoreManager = KeyStoreManager()
        val passphraseStore = SecurePassphraseStore(context, keyStoreManager)
        val db = SecureDatabase.create(context = context.applicationContext, passphraseStore = passphraseStore)
        return NotesRepositoryImpl(db.noteDao())
    }
}
