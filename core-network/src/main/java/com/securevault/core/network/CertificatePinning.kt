package com.securevault.core.network

import okhttp3.CertificatePinner

object CertificatePinning {
    private const val HOST = "api.github.com"

    val certificatePinner: CertificatePinner =
        CertificatePinner
            .Builder()
            .add(HOST, "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .add(HOST, "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build()
}
