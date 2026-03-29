package com.openclash.portal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openclash.portal.data.HttpClientFactory
import com.openclash.portal.data.LuciLoginParser
import com.openclash.portal.data.OpenClashUrlResolver
import com.openclash.portal.data.PersistentCookieJar
import com.openclash.portal.data.RouterDiscoveryRepository
import com.openclash.portal.data.RouterPreferencesStore
import com.openclash.portal.data.SecureStorage
import com.openclash.portal.data.SessionRepository
import com.openclash.portal.model.AppLanguage
import com.openclash.portal.ui.MainViewModel
import com.openclash.portal.ui.MainViewModelFactory
import com.openclash.portal.ui.OpenClashPortalApp
import com.openclash.portal.ui.theme.OpenClashPortalTheme
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val preferencesStore = RouterPreferencesStore(applicationContext)
        val initialLanguage = runBlocking { preferencesStore.loadAppLanguage() }
        applyLanguage(initialLanguage)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val secureStorage = SecureStorage(applicationContext)
        val cookieJar = PersistentCookieJar(secureStorage)
        val sessionRepository = SessionRepository(
            preferencesStore = preferencesStore,
            secureStorage = secureStorage,
            cookieJar = cookieJar,
            httpClientFactory = HttpClientFactory(cookieJar),
            loginParser = LuciLoginParser(),
            urlResolver = OpenClashUrlResolver(),
        )
        val discoveryRepository = RouterDiscoveryRepository(applicationContext)

        setContent {
            OpenClashPortalTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(
                        sessionRepository = sessionRepository,
                        discoveryRepository = discoveryRepository,
                        onLanguageChanged = ::applyLanguage,
                    ),
                )
                OpenClashPortalApp(viewModel = viewModel)
            }
        }
    }

    private fun applyLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }
}
