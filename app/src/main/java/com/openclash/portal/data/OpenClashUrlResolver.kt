package com.openclash.portal.data

import com.openclash.portal.model.OpenClashStatus
import com.openclash.portal.model.ResolvedPortalUrls
import com.openclash.portal.model.RouterProfile
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class OpenClashUrlResolver {
    fun resolve(profile: RouterProfile, status: OpenClashStatus): ResolvedPortalUrls {
        val openClashUrl = profile.customOpenClashUrl?.takeIf { it.isNotBlank() } ?: profile.openClashUrl()

        val hasForward = !status.dbForwardDomain.isNullOrBlank() && status.dbForwardPort != null
        val uiScheme = if (hasForward && status.dbForwardSsl) "https" else "http"
        val uiHost = if (hasForward) status.dbForwardDomain!! else profile.normalizedHost
        val uiPort = if (hasForward) status.dbForwardPort!! else status.cnPort
        val secret = status.dase?.takeIf { it.isNotBlank() }
        val controllerHost = if (hasForward) status.dbForwardDomain!! else (status.daip ?: profile.normalizedHost)
        val controllerPort = if (hasForward) status.dbForwardPort!! else status.cnPort

        return ResolvedPortalUrls(
            openClashUrl = openClashUrl,
            zashboardUrl = profile.customZashboardUrl?.takeIf { it.isNotBlank() }
                ?: if (status.zashboardAvailable) {
                    buildDashboardUrl(
                        scheme = uiScheme,
                        host = uiHost,
                        port = uiPort,
                        path = "zashboard",
                        hostParameterName = "hostname",
                        controllerHost = controllerHost,
                        controllerPort = controllerPort,
                        secret = secret,
                    )
                } else {
                    null
                },
            metaCubeXdUrl = profile.customMetaCubeXdUrl?.takeIf { it.isNotBlank() }
                ?: if (status.metaCubeXdAvailable) {
                    buildDashboardUrl(
                        scheme = uiScheme,
                        host = uiHost,
                        port = uiPort,
                        path = "metacubexd",
                        hostParameterName = "hostname",
                        controllerHost = controllerHost,
                        controllerPort = controllerPort,
                        secret = secret,
                    )
                } else {
                    null
                },
        )
    }

    private fun buildDashboardUrl(
        scheme: String,
        host: String,
        port: Int,
        path: String,
        hostParameterName: String,
        controllerHost: String,
        controllerPort: Int,
        secret: String?,
    ): String {
        val base = "$scheme://$host:$port/ui/$path"
        return if (secret.isNullOrBlank()) {
            "$base/"
        } else {
            val encodedHost = controllerHost.urlEncode()
            val encodedSecret = secret.urlEncode()
            "$base/#/setup?$hostParameterName=$encodedHost&port=$controllerPort&secret=$encodedSecret"
        }
    }
}

private fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

