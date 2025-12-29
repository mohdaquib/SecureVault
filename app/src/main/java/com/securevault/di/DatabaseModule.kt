package com.securevault.di

import android.content.Context
import com.securevault.core.crypto.SecurePassphraseStore
import com.securevault.data.local.NoteDao
import com.securevault.data.local.SecureDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideSecureDatabase(
        @ApplicationContext context: Context,
        passphraseStore: SecurePassphraseStore,
    ): SecureDatabase = SecureDatabase.create(context = context, passphraseStore = passphraseStore)

    @Provides
    @Singleton
    fun provideNoteDao(db: SecureDatabase): NoteDao = db.noteDao()
}
