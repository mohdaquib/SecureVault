package com.securevault.core.network

class SecurityHealthChecker(
    private val api: HealthApi,
) {
    suspend fun check(): NetworkResult<Unit> =
        safeNetworkCall {
            api.healthCheck()
            Unit
        }
}
