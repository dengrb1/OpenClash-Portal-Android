package com.openclash.portal.model

import org.json.JSONObject

data class OpenClashStatus(
    val clash: Boolean,
    val daip: String?,
    val dase: String?,
    val cnPort: Int,
    val dbForwardDomain: String?,
    val dbForwardPort: Int?,
    val dbForwardSsl: Boolean,
    val zashboardAvailable: Boolean,
    val metaCubeXdAvailable: Boolean,
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): OpenClashStatus {
            return OpenClashStatus(
                clash = jsonObject.optBoolean("clash", false),
                daip = jsonObject.optString("daip").ifBlank { null },
                dase = jsonObject.optString("dase").ifBlank { null },
                cnPort = jsonObject.optInt("cn_port", 9090),
                dbForwardDomain = jsonObject.optString("db_foward_domain").ifBlank { null },
                dbForwardPort = jsonObject.optInt("db_foward_port").takeIf { it > 0 },
                dbForwardSsl = jsonObject.optInt("db_forward_ssl", 0) == 1 || jsonObject.optBoolean("db_forward_ssl", false),
                zashboardAvailable = jsonObject.optBoolean("zashboard", false),
                metaCubeXdAvailable = jsonObject.optBoolean("metacubexd", false),
            )
        }
    }
}

