package com.openclash.portal.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.openclash.portal.model.RouterProtocol
import java.util.LinkedHashSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Request

class RouterDiscoveryRepository(
    private val context: Context,
) {
    private val probeClientFactory = HttpClientFactory(PersistentCookieJar(SecureStorage(context)))

    suspend fun discover(protocol: RouterProtocol): List<String> = coroutineScope {
        val candidates = LinkedHashSet<String>()
        findGateway()?.let(candidates::add)
        candidates += listOf("192.168.1.1", "192.168.0.1", "192.168.31.1", "10.0.0.1")

        candidates.map { host ->
            async {
                host.takeIf { probe(protocol, it) }
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun probe(protocol: RouterProtocol, host: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("${protocol.scheme}://$host/cgi-bin/luci/")
                .get()
                .build()
            probeClientFactory.create(
                includeCookies = false,
                connectTimeoutSeconds = 2,
                readTimeoutSeconds = 2,
            ).newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                response.isSuccessful && (
                    body.contains("LuCI", ignoreCase = true) ||
                        body.contains("luci_password", ignoreCase = true) ||
                        body.contains("openwrt", ignoreCase = true)
                    )
            }
        }.getOrElse { false }
    }

    private fun findGateway(): String? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return null
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return null
        val linkProperties = connectivityManager.getLinkProperties(network) ?: return null
        return linkProperties.routes
            .firstOrNull { route ->
                val gateway = route.gateway ?: return@firstOrNull false
                !gateway.isAnyLocalAddress
            }
            ?.gateway
            ?.hostAddress
    }
}

