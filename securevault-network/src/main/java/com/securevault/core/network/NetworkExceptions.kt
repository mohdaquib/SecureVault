package com.securevault.core.network

sealed class NetworkException(
    message: String,
) : Exception(message)

class PinningFailureException : NetworkException("SSL certificate pinning validation failed")

class NetworkUnavailableException : NetworkException("Network unavailable")
