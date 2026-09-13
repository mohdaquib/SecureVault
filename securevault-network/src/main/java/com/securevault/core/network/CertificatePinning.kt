package com.securevault.core.network

import okhttp3.CertificatePinner

object CertificatePinning {
    private const val HOST = "api.github.com"

    val certificatePinner: CertificatePinner =
        CertificatePinner
            .Builder()
            .add(HOST, "sha256/1EkvzibgiE3k+xdsv+7UU5vhV8kdFCQiUiFdMX5Guuk=")
            .build()
}
