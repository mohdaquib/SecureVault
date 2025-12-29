package com.securevault.di

import android.content.Context
import com.securevault.core.crypto.KeyStoreManager
import com.securevault.core.crypto.SecurePassphraseStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CryptoModule {
    @Provides
    @Singleton
    fun provideKeyStoreManager(): KeyStoreManager = KeyStoreManager()

    @Provides
    @Singleton
    fun provideSecurePassphraseStore(@ApplicationContext context: Context, keyStoreManager: KeyStoreManager):
            SecurePassphraseStore = SecurePassphraseStore(context = context, keyStoreManager = keyStoreManager)
}