package com.securevault.core.network

import com.securevault.core.network.TestCertificates.clientHandshakeCertificates
import com.securevault.core.network.TestCertificates.serverHandshakeCertificates
import com.securevault.core.network.TestCertificates.validPin
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class CertificatePinningTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.useHttps(serverHandshakeCertificates.sslSocketFactory(), false)
        server.start(0)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun request_succeeds_when_certificate_pin_matches() {
        val host = server.hostName
        val client =
            OkHttpClient
                .Builder()
                .sslSocketFactory(
                    clientHandshakeCertificates.sslSocketFactory(),
                    clientHandshakeCertificates.trustManager,
                ).certificatePinner(CertificatePinner.Builder().add(host, validPin).build())
                .build()
        server.enqueue(MockResponse().setBody("OK"))
        val request = Request.Builder().url(server.url("/")).build()
        val response = client.newCall(request).execute()
        assertTrue(response.isSuccessful)
    }
}
