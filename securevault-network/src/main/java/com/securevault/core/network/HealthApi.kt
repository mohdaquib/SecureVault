package com.securevault.core.network

import retrofit2.Response
import retrofit2.http.GET

internal interface HealthApi {
    @GET("zen")
    suspend fun healthCheck(): Response<Unit>
}
