package com.openclash.portal.model

data class ResolvedPortalUrls(
    val openClashUrl: String,
    val zashboardUrl: String?,
    val metaCubeXdUrl: String?,
) {
    fun urlFor(destination: PortalDestination): String? {
        return when (destination) {
            PortalDestination.OPENCLASH -> openClashUrl
            PortalDestination.ZASHBOARD -> zashboardUrl
            PortalDestination.METACUBEXD -> metaCubeXdUrl
        }
    }
}
