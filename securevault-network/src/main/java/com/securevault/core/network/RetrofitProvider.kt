package com.securevault.core.network

import retrofit2.Retrofit

internal class RetrofitProvider(
    private val okHttpProvider: OkHttpProvider,
) {
    fun createHealthApi(): HealthApi =
        Retrofit
            .Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpProvider.createPinnedClient())
            .build()
            .create(HealthApi::class.java)
}
