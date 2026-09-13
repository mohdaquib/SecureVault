package com.securevault.demo.data

import android.content.Context
import com.securevault.core.crypto.KeyStoreManager
import com.securevault.core.crypto.SecurePassphraseStore
import com.securevault.demo.data.local.SecureDatabase
import com.securevault.demo.data.repository.NotesRepositoryImpl
import com.securevault.demo.domain.repository.NotesRepository

object SecureVaultDataFactory {
    fun createNotesRepository(context: Context): NotesRepository {
        val keyStoreManager = KeyStoreManager()
        val passphraseStore = SecurePassphraseStore(context, keyStoreManager)
        val db = SecureDatabase.create(context = context.applicationContext, passphraseStore = passphraseStore)
        return NotesRepositoryImpl(db.noteDao())
    }
}
