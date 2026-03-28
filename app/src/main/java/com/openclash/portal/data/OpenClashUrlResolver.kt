package com.openclash.portal.data

import com.openclash.portal.model.OpenClashStatus
import com.openclash.portal.model.ResolvedPortalUrls
import com.openclash.portal.model.RouterProfile

class OpenClashUrlResolver {
    fun resolve(profile: RouterProfile, status: OpenClashStatus): ResolvedPortalUrls {
        val openClashUrl = profile.customOpenClashUrl?.takeIf { it.isNotBlank() } ?: profile.openClashUrl()

        val hasForward = !status.dbForwardDomain.isNullOrBlank() && status.dbForwardPort != null
        val dashboardScheme = if (hasForward && status.dbForwardSsl) "https" else "http"
        val dashboardHost = if (hasForward) status.dbForwardDomain!! else profile.normalizedHost
        val dashboardPort = if (hasForward) status.dbForwardPort!! else status.cnPort
        val secret = status.dase?.takeIf { it.isNotBlank() }

        return ResolvedPortalUrls(
            openClashUrl = openClashUrl,
            zashboardUrl = profile.customZashboardUrl?.takeIf { it.isNotBlank() }
                ?: if (status.zashboardAvailable) buildDashboardUrl(dashboardScheme, dashboardHost, dashboardPort, "zashboard", secret) else null,
            metaCubeXdUrl = profile.customMetaCubeXdUrl?.takeIf { it.isNotBlank() }
                ?: if (status.metaCubeXdAvailable) buildDashboardUrl(dashboardScheme, dashboardHost, dashboardPort, "metacubexd", secret) else null,
        )
    }

    private fun buildDashboardUrl(
        scheme: String,
        host: String,
        port: Int,
        panel: String,
        secret: String?,
    ): String {
        val base = "$scheme://$host:$port/ui/$panel"
        return if (secret.isNullOrBlank()) {
            "$base/#/"
        } else {
            "$base/#/setup?hostname=$host&port=$port&secret=$secret"
        }
    }
}

