package com.securevault.core.network

class SecurityHealthChecker internal constructor(
    private val api: HealthApi,
) {
    constructor() : this(RetrofitProvider(OkHttpProvider()).createHealthApi())

    suspend fun check(): NetworkResult<Unit> =
        safeNetworkCall {
            val response = api.healthCheck()
            if (!response.isSuccessful) {
                throw IllegalStateException("Health check failed: ${response.code()}")
            }
            Unit
        }
}
