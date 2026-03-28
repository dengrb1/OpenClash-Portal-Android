package com.openclash.portal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.openclash.portal.data.ConnectError
import com.openclash.portal.data.ConnectResult
import com.openclash.portal.data.RouterDiscoveryRepository
import com.openclash.portal.data.SessionRepository
import com.openclash.portal.model.OpenClashStatus
import com.openclash.portal.model.PortalDestination
import com.openclash.portal.model.ResolvedPortalUrls
import com.openclash.portal.model.RouterProfile
import com.openclash.portal.model.RouterProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val sessionRepository: SessionRepository,
    private val discoveryRepository: RouterDiscoveryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        bootstrap()
    }

    fun onProtocolSelected(protocol: RouterProtocol) {
        _uiState.update { state ->
            val resetPort = state.portInput.isBlank() || state.portInput == state.protocol.defaultPort.toString()
            state.copy(
                protocol = protocol,
                portInput = if (resetPort) protocol.defaultPort.toString() else state.portInput,
            )
        }
        discoverRouters()
    }

    fun onHostChanged(value: String) {
        _uiState.update { it.copy(hostInput = value, connectionError = null) }
    }

    fun onPortChanged(value: String) {
        _uiState.update { it.copy(portInput = value.filter(Char::isDigit), connectionError = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(passwordInput = value, connectionError = null) }
    }

    fun onOpenClashUrlChanged(value: String) {
        _uiState.update { it.copy(customOpenClashUrl = value) }
    }

    fun onZashboardUrlChanged(value: String) {
        _uiState.update { it.copy(customZashboardUrl = value) }
    }

    fun onMetaCubeXdUrlChanged(value: String) {
        _uiState.update { it.copy(customMetaCubeXdUrl = value) }
    }

    fun connect() {
        val profile = buildProfileOrNull() ?: run {
            _uiState.update { it.copy(connectionError = "Enter a valid router address.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, connectionError = null) }
            sessionRepository.saveProfile(profile, _uiState.value.passwordInput)
            when (val result = sessionRepository.connect(profile, _uiState.value.passwordInput, _uiState.value.trustedHosts)) {
                is ConnectResult.Success -> {
                    sessionRepository.saveProfile(profile, _uiState.value.passwordInput)
                    val urls = listOfNotNull(
                        result.urls.openClashUrl,
                        result.urls.zashboardUrl,
                        result.urls.metaCubeXdUrl,
                    )
                    sessionRepository.syncCookiesToWebView(urls)
                    _uiState.update {
                        it.copy(
                            isInitializing = false,
                            isConnecting = false,
                            isConnected = true,
                            activeProfile = profile,
                            status = result.status,
                            resolvedUrls = result.urls,
                            connectionError = null,
                        )
                    }
                }

                is ConnectResult.SslUntrusted -> {
                    _uiState.update {
                        it.copy(
                            isInitializing = false,
                            isConnecting = false,
                            pendingSslHost = result.host,
                        )
                    }
                }

                is ConnectResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isInitializing = false,
                            isConnecting = false,
                            isConnected = false,
                            connectionError = result.error.toUiMessage(),
                        )
                    }
                }
            }
        }
    }

    fun discoverRouters() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscovering = true) }
            val candidates = discoveryRepository.discover(_uiState.value.protocol)
            _uiState.update { state ->
                state.copy(
                    isDiscovering = false,
                    discoveryCandidates = candidates,
                    hostInput = state.hostInput.ifBlank { candidates.firstOrNull().orEmpty() },
                )
            }
        }
    }

    fun selectDiscoveryCandidate(candidate: String) {
        _uiState.update { it.copy(hostInput = candidate) }
    }

    fun selectTab(destination: PortalDestination) {
        _uiState.update { it.copy(selectedTab = destination, pageError = null) }
    }

    fun setPageError(error: String?) {
        _uiState.update { it.copy(pageError = error) }
    }

    fun syncCookiesFromWebView(url: String) {
        sessionRepository.syncCookiesFromWebView(url)
    }

    fun trustPendingHostAndRetry() {
        val host = _uiState.value.pendingSslHost ?: return
        viewModelScope.launch {
            sessionRepository.trustHost(host)
            _uiState.update {
                it.copy(
                    trustedHosts = it.trustedHosts + host.lowercase(),
                    pendingSslHost = null,
                )
            }
            connect()
        }
    }

    fun trustHost(host: String) {
        viewModelScope.launch {
            sessionRepository.trustHost(host)
            _uiState.update { it.copy(trustedHosts = it.trustedHosts + host.lowercase()) }
        }
    }

    fun dismissSslPrompt() {
        _uiState.update { it.copy(pendingSslHost = null) }
    }

    fun openSettings() {
        _uiState.update { it.copy(showSettings = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(showSettings = false) }
    }

    fun saveSettingsAndReconnect() {
        _uiState.update { it.copy(showSettings = false) }
        connect()
    }

    fun clearSession() {
        viewModelScope.launch {
            sessionRepository.clearSession(clearTrustedHosts = false)
            _uiState.update {
                it.copy(
                    isConnected = false,
                    resolvedUrls = null,
                    status = null,
                    pageError = null,
                )
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            sessionRepository.clearSession(clearTrustedHosts = true)
            _uiState.value = MainUiState()
            bootstrap()
        }
    }

    private fun bootstrap() {
        viewModelScope.launch {
            val savedProfile = sessionRepository.restoreProfile()
            val password = sessionRepository.loadPassword()
            val trustedHosts = sessionRepository.loadTrustedHosts()
            _uiState.update {
                it.copy(
                    protocol = savedProfile?.protocol ?: RouterProtocol.HTTP,
                    hostInput = savedProfile?.normalizedHost.orEmpty(),
                    portInput = (savedProfile?.port ?: RouterProtocol.HTTP.defaultPort).toString(),
                    passwordInput = password,
                    customOpenClashUrl = savedProfile?.customOpenClashUrl.orEmpty(),
                    customZashboardUrl = savedProfile?.customZashboardUrl.orEmpty(),
                    customMetaCubeXdUrl = savedProfile?.customMetaCubeXdUrl.orEmpty(),
                    activeProfile = savedProfile,
                    trustedHosts = trustedHosts,
                )
            }
            discoverRouters()
            if (savedProfile != null && password.isNotBlank()) {
                connect()
            } else {
                _uiState.update { it.copy(isInitializing = false) }
            }
        }
    }

    private fun buildProfileOrNull(): RouterProfile? {
        return runCatching {
            RouterProfile.fromUserInput(
                protocol = _uiState.value.protocol,
                hostInput = _uiState.value.hostInput,
                portInput = _uiState.value.portInput,
                customOpenClashUrl = _uiState.value.customOpenClashUrl,
                customZashboardUrl = _uiState.value.customZashboardUrl,
                customMetaCubeXdUrl = _uiState.value.customMetaCubeXdUrl,
            )
        }.getOrNull()?.takeIf { it.normalizedHost.isNotBlank() }
    }
}

data class MainUiState(
    val isInitializing: Boolean = true,
    val isDiscovering: Boolean = false,
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val protocol: RouterProtocol = RouterProtocol.HTTP,
    val hostInput: String = "",
    val portInput: String = RouterProtocol.HTTP.defaultPort.toString(),
    val passwordInput: String = "",
    val customOpenClashUrl: String = "",
    val customZashboardUrl: String = "",
    val customMetaCubeXdUrl: String = "",
    val discoveryCandidates: List<String> = emptyList(),
    val selectedTab: PortalDestination = PortalDestination.OPENCLASH,
    val resolvedUrls: ResolvedPortalUrls? = null,
    val status: OpenClashStatus? = null,
    val activeProfile: RouterProfile? = null,
    val trustedHosts: Set<String> = emptySet(),
    val connectionError: String? = null,
    val pageError: String? = null,
    val showSettings: Boolean = false,
    val pendingSslHost: String? = null,
)

private fun ConnectError.toUiMessage(): String {
    return when (this) {
        ConnectError.InvalidCredentials -> "Login failed: password rejected or LuCI login validation failed."
        ConnectError.OpenClashUnavailable -> "OpenClash page is unavailable. The router may not have OpenClash installed."
        ConnectError.RouterUnreachable -> "Cannot reach the router. Check the address, protocol, and current network."
        is ConnectError.Unknown -> message
    }
}

class MainViewModelFactory(
    private val sessionRepository: SessionRepository,
    private val discoveryRepository: RouterDiscoveryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(
            sessionRepository = sessionRepository,
            discoveryRepository = discoveryRepository,
        ) as T
    }
}
