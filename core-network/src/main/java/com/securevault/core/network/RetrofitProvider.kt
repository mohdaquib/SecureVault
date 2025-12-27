package com.securevault.core.network

import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class RetrofitProvider(
    private val okHttpProvider: OkHttpProvider,
) {
    fun createHealthApi(): HealthApi =
        Retrofit
            .Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpProvider.createPinnedClient())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(HealthApi::class.java)
}
