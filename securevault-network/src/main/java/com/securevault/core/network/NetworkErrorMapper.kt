package com.securevault.core.network

import okio.IOException
import javax.net.ssl.SSLPeerUnverifiedException

object NetworkErrorMapper {
    fun map(throwable: Throwable): NetworkError =
        when (throwable) {
            is SSLPeerUnverifiedException -> NetworkError.PinningFailure
            is IOException -> NetworkError.NetworkUnavailable
            else -> NetworkError.Unknown(throwable = throwable)
        }
}
