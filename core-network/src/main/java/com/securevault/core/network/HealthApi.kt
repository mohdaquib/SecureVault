package com.securevault.core.network

import retrofit2.http.GET

interface HealthApi {
    @GET("zen")
    suspend fun healthCheck(): String
}