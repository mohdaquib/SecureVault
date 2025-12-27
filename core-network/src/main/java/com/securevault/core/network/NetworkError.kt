package com.securevault.core.network

sealed class NetworkError {
    object PinningFailure : NetworkError()
    object NetworkUnavailable : NetworkError()
    data class Unknown(val throwable: Throwable) : NetworkError()
}