package com.securevault.core.network

import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.security.MessageDigest
import java.util.Base64

object TestCertificates {
    val serverCertificate: HeldCertificate =
        HeldCertificate
            .Builder()
            .addSubjectAlternativeName("localhost")
            .addSubjectAlternativeName("127.0.0.1")
            .commonName("localhost")
            .build()
    val serverHandshakeCertificates: HandshakeCertificates =
        HandshakeCertificates
            .Builder()
            .heldCertificate(serverCertificate)
            .build()

    val clientHandshakeCertificates: HandshakeCertificates =
        HandshakeCertificates
            .Builder()
            .addTrustedCertificate(serverCertificate.certificate)
            .build()
    val validPin: String = "sha256/${serverCertificate.certificate.publicKey.encoded.sha256Base64()}"
}

fun ByteArray.sha256Base64(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(this)
    return Base64.getEncoder().encodeToString(digest)
}
