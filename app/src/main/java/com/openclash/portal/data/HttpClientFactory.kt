package com.openclash.portal.data

import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttpClient

class HttpClientFactory(
    private val cookieJar: PersistentCookieJar,
) {
    fun create(
        allowUnsafeSslForHosts: Set<String> = emptySet(),
        includeCookies: Boolean = true,
        connectTimeoutSeconds: Long = 4,
        readTimeoutSeconds: Long = 6,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)

        if (includeCookies) {
            builder.cookieJar(cookieJar)
        }

        if (allowUnsafeSslForHosts.isNotEmpty()) {
            builder.sslSocketFactory(unsafeSocketFactory, unsafeTrustManager)
            builder.hostnameVerifier(TrustedHostVerifier(allowUnsafeSslForHosts))
        }

        return builder.build()
    }

    private class TrustedHostVerifier(
        private val trustedHosts: Set<String>,
    ) : HostnameVerifier {
        override fun verify(hostname: String?, session: SSLSession?): Boolean {
            return hostname != null && trustedHosts.contains(hostname.lowercase())
        }
    }

    private companion object {
        val unsafeTrustManager: X509TrustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        val unsafeSocketFactory: SSLSocketFactory by lazy {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(unsafeTrustManager), SecureRandom())
            sslContext.socketFactory
        }
    }
}

