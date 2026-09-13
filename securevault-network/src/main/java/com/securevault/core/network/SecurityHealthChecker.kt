package com.securevault.core.network

class SecurityHealthChecker(
    private val api: HealthApi,
) {
    suspend fun check(): NetworkResult<Unit> =
        safeNetworkCall {
            val response = api.healthCheck()
            if (!response.isSuccessful) {
                throw IllegalStateException("Health check failed: ${response.code()}")
            }
            Unit
        }
}
