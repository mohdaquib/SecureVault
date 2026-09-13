package com.securevault.core.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class OkHttpProvider {
    fun createPinnedClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .certificatePinner(CertificatePinning.certificatePinner)
            .addInterceptor(UserAgentInterceptor())
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
}
