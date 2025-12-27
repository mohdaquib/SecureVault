package com.securevault.core.network

suspend inline fun <T> safeNetworkCall(
    crossinline block: suspend () -> T
) : NetworkResult<T> {
    return try {
        NetworkResult.Success(block())
    } catch (throwable: Throwable) {
        NetworkResult.Failure(NetworkErrorMapper.map(throwable))
    }
}