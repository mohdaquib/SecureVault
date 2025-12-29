package com.securevault.di

import com.securevault.core.network.HealthApi
import com.securevault.core.network.OkHttpProvider
import com.securevault.core.network.RetrofitProvider
import com.securevault.core.network.SecurityHealthChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpProvider() = OkHttpProvider()

    @Provides
    @Singleton
    fun provideHealthApi(okHttpProvider: OkHttpProvider): HealthApi = RetrofitProvider(okHttpProvider).createHealthApi()

    @Provides
    @Singleton
    fun provideSecurityHealthChecker(api: HealthApi): SecurityHealthChecker = SecurityHealthChecker(api)
}
