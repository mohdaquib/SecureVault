package com.securevault.core.network

import android.net.Network

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Failure(val error: NetworkError) : NetworkResult<Nothing>()
}