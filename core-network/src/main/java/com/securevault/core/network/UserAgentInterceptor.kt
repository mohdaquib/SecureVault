package com.securevault.core.network

import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request =
            chain
                .request()
                .newBuilder()
                .header("User-Agent", "SecureVault-Android")
                .build()
        return chain.proceed(request)
    }
}
