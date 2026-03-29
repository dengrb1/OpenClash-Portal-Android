package com.openclash.portal.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.openclash.portal.model.AppLanguage
import com.openclash.portal.model.RouterProfile
import com.openclash.portal.model.RouterProtocol
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.routerDataStore by preferencesDataStore(name = "router_profile")

class RouterPreferencesStore(
    private val context: Context,
) {
    private object Keys {
        val protocol = stringPreferencesKey("protocol")
        val host = stringPreferencesKey("host")
        val port = intPreferencesKey("port")
        val customOpenClashUrl = stringPreferencesKey("custom_openclash_url")
        val customZashboardUrl = stringPreferencesKey("custom_zashboard_url")
        val customMetaCubeXdUrl = stringPreferencesKey("custom_metacubexd_url")
        val trustedHosts = stringSetPreferencesKey("trusted_hosts")
        val appLanguage = stringPreferencesKey("app_language")
    }

    suspend fun loadProfile(): RouterProfile? {
        val preferences = context.routerDataStore.data.first()
        val host = preferences[Keys.host] ?: return null
        val protocol = preferences[Keys.protocol]?.let(RouterProtocol::valueOf) ?: RouterProtocol.HTTP
        return RouterProfile(
            protocol = protocol,
            host = host,
            port = preferences[Keys.port] ?: protocol.defaultPort,
            customOpenClashUrl = preferences[Keys.customOpenClashUrl],
            customZashboardUrl = preferences[Keys.customZashboardUrl],
            customMetaCubeXdUrl = preferences[Keys.customMetaCubeXdUrl],
        )
    }

    suspend fun saveProfile(profile: RouterProfile) {
        context.routerDataStore.edit { preferences ->
            preferences[Keys.protocol] = profile.protocol.name
            preferences[Keys.host] = profile.normalizedHost
            preferences[Keys.port] = profile.port
            if (profile.customOpenClashUrl.isNullOrBlank()) {
                preferences.remove(Keys.customOpenClashUrl)
            } else {
                preferences[Keys.customOpenClashUrl] = profile.customOpenClashUrl
            }
            if (profile.customZashboardUrl.isNullOrBlank()) {
                preferences.remove(Keys.customZashboardUrl)
            } else {
                preferences[Keys.customZashboardUrl] = profile.customZashboardUrl
            }
            if (profile.customMetaCubeXdUrl.isNullOrBlank()) {
                preferences.remove(Keys.customMetaCubeXdUrl)
            } else {
                preferences[Keys.customMetaCubeXdUrl] = profile.customMetaCubeXdUrl
            }
        }
    }

    suspend fun loadTrustedHosts(): Set<String> {
        return context.routerDataStore.data
            .map { preferences -> preferences[Keys.trustedHosts].orEmpty() }
            .first()
    }

    suspend fun trustHost(host: String) {
        context.routerDataStore.edit { preferences ->
            val updated = preferences[Keys.trustedHosts].orEmpty().toMutableSet()
            updated += host.lowercase()
            preferences[Keys.trustedHosts] = updated
        }
    }

    suspend fun clearTrustedHosts() {
        context.routerDataStore.edit { preferences ->
            preferences.remove(Keys.trustedHosts)
        }
    }

    suspend fun loadAppLanguage(): AppLanguage {
        val tag = context.routerDataStore.data
            .map { preferences -> preferences[Keys.appLanguage] }
            .first()
        return AppLanguage.entries.firstOrNull { it.tag == tag } ?: AppLanguage.SIMPLIFIED_CHINESE
    }

    suspend fun saveAppLanguage(language: AppLanguage) {
        context.routerDataStore.edit { preferences ->
            preferences[Keys.appLanguage] = language.tag
        }
    }
}

