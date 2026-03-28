package com.openclash.portal.model

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class RouterProfile(
    val protocol: RouterProtocol,
    val host: String,
    val port: Int,
    val username: String = "root",
    val encryptedPassword: String? = null,
    val customOpenClashUrl: String? = null,
    val customZashboardUrl: String? = null,
    val customMetaCubeXdUrl: String? = null,
) {
    val normalizedHost: String = host.trim()
        .removePrefix("http://")
        .removePrefix("https://")
        .trim('/')

    val baseUrl: String
        get() = buildString {
            append(protocol.scheme)
            append("://")
            append(normalizedHost)
            if (port != protocol.defaultPort) {
                append(":")
                append(port)
            }
        }

    fun openClashUrl(): String = customOpenClashUrl?.takeIf { it.isNotBlank() }
        ?: "$baseUrl/cgi-bin/luci/admin/services/openclash/client"

    companion object {
        fun fromUserInput(
            protocol: RouterProtocol,
            hostInput: String,
            portInput: String,
            customOpenClashUrl: String?,
            customZashboardUrl: String?,
            customMetaCubeXdUrl: String?,
        ): RouterProfile {
            val trimmed = hostInput.trim()
            val parsedUrl = trimmed.toHttpUrlOrNull()
            val effectiveProtocol = when {
                parsedUrl == null -> protocol
                parsedUrl.isHttps -> RouterProtocol.HTTPS
                else -> RouterProtocol.HTTP
            }

            val rawHost = when {
                parsedUrl != null -> parsedUrl.host
                trimmed.contains("://") -> trimmed.substringAfter("://").substringBefore("/")
                else -> trimmed.substringBefore("/")
            }
            val explicitPort = when {
                portInput.isNotBlank() -> portInput.toIntOrNull()
                parsedUrl?.port != null && parsedUrl.port != parsedUrl.defaultPort -> parsedUrl.port
                rawHost.count { it == ':' } == 1 && !rawHost.startsWith("[") -> rawHost.substringAfterLast(":").toIntOrNull()
                else -> null
            }
            val sanitizedHost = if (rawHost.count { it == ':' } == 1 && !rawHost.startsWith("[")) {
                rawHost.substringBeforeLast(":")
            } else {
                rawHost
            }

            return RouterProfile(
                protocol = effectiveProtocol,
                host = sanitizedHost.ifBlank { rawHost },
                port = explicitPort ?: effectiveProtocol.defaultPort,
                customOpenClashUrl = customOpenClashUrl?.trim()?.ifBlank { null },
                customZashboardUrl = customZashboardUrl?.trim()?.ifBlank { null },
                customMetaCubeXdUrl = customMetaCubeXdUrl?.trim()?.ifBlank { null },
            )
        }
    }
}

