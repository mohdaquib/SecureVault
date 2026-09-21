package com.securevault.di

import com.securevault.core.network.SecurityHealthChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun provideSecurityHealthChecker(): SecurityHealthChecker = SecurityHealthChecker()
}
