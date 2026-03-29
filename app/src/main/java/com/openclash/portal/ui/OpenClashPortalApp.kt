@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.openclash.portal.ui

import android.annotation.SuppressLint
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.openclash.portal.model.PortalDestination
import com.openclash.portal.model.RouterProtocol
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Composable
fun OpenClashPortalApp(
    viewModel: MainViewModel,
) {
    val state by viewModel.uiState.collectAsState()

    if (state.pendingSslHost != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSslPrompt,
            confirmButton = {
                TextButton(onClick = viewModel::trustPendingHostAndRetry) {
                    Text("Trust and continue")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissSslPrompt) {
                    Text("Cancel")
                }
            },
            title = { Text("Untrusted HTTPS certificate") },
            text = { Text("Allow insecure HTTPS only for router host ${state.pendingSslHost}.") },
        )
    }

    if (!state.isConnected) {
        ConnectionScreen(
            state = state,
            onProtocolSelected = viewModel::onProtocolSelected,
            onHostChanged = viewModel::onHostChanged,
            onPortChanged = viewModel::onPortChanged,
            onPasswordChanged = viewModel::onPasswordChanged,
            onConnect = viewModel::connect,
            onDiscover = viewModel::discoverRouters,
            onCandidateSelected = viewModel::selectDiscoveryCandidate,
        )
    } else {
        PortalScreen(
            state = state,
            onSelectTab = viewModel::selectTab,
            onOpenSettings = viewModel::openSettings,
            onCloseSettings = viewModel::closeSettings,
            onSyncCookies = viewModel::syncCookiesFromWebView,
            onSetPageError = viewModel::setPageError,
            onReconnect = viewModel::connect,
            onLogout = viewModel::clearSession,
            onWipeAll = viewModel::clearAllData,
            onProtocolSelected = viewModel::onProtocolSelected,
            onHostChanged = viewModel::onHostChanged,
            onPortChanged = viewModel::onPortChanged,
            onPasswordChanged = viewModel::onPasswordChanged,
            onOpenClashUrlChanged = viewModel::onOpenClashUrlChanged,
            onZashboardUrlChanged = viewModel::onZashboardUrlChanged,
            onMetaCubeXdUrlChanged = viewModel::onMetaCubeXdUrlChanged,
            onSaveSettings = viewModel::saveSettingsAndReconnect,
            onTrustHost = viewModel::trustHost,
        )
    }
}

@Composable
private fun ConnectionScreen(
    state: MainUiState,
    onProtocolSelected: (RouterProtocol) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConnect: () -> Unit,
    onDiscover: () -> Unit,
    onCandidateSelected: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OpenClash Portal") },
                actions = {
                    TextButton(onClick = onDiscover, enabled = !state.isDiscovering) {
                        Text("Discover")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Log in to OpenClash directly and switch to Zashboard or MetaCubeXD inside the app.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RouterProtocol.entries.forEach { protocol ->
                        FilterChip(
                            selected = state.protocol == protocol,
                            onClick = { onProtocolSelected(protocol) },
                            label = { Text(protocol.name) },
                        )
                    }
                }
                if (state.discoveryCandidates.isNotEmpty()) {
                    Text(
                        text = "Discovered router addresses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.discoveryCandidates.forEach { candidate ->
                            AssistChip(
                                onClick = { onCandidateSelected(candidate) },
                                label = { Text(candidate) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = state.hostInput,
                    onValueChange = onHostChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Router host or full URL") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.portInput,
                    onValueChange = onPortChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("LuCI port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = state.passwordInput,
                    onValueChange = onPasswordChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("OpenWrt root password") },
                    singleLine = true,
                )
                state.connectionError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Button(
                    onClick = onConnect,
                    enabled = !state.isConnecting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isConnecting || state.isInitializing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(if (state.isConnecting) "Connecting..." else "Connect and open OpenClash")
                }
            }
        }
    }
}

@Composable
private fun PortalScreen(
    state: MainUiState,
    onSelectTab: (PortalDestination) -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onSyncCookies: (String) -> Unit,
    onSetPageError: (String?) -> Unit,
    onReconnect: () -> Unit,
    onLogout: () -> Unit,
    onWipeAll: () -> Unit,
    onProtocolSelected: (RouterProtocol) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onOpenClashUrlChanged: (String) -> Unit,
    onZashboardUrlChanged: (String) -> Unit,
    onMetaCubeXdUrlChanged: (String) -> Unit,
    onSaveSettings: () -> Unit,
    onTrustHost: (String) -> Unit,
) {
    val currentUrl = state.resolvedUrls?.urlFor(state.selectedTab)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.activeProfile?.normalizedHost ?: "OpenClash Portal") },
                actions = {
                    TextButton(onClick = onReconnect) { Text("Reconnect") }
                    TextButton(onClick = onOpenSettings) { Text("Settings") }
                    TextButton(onClick = onLogout) { Text("Logout") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TabRow(selectedTabIndex = state.selectedTab.ordinal) {
                PortalDestination.entries.forEach { destination ->
                    Tab(
                        selected = state.selectedTab == destination,
                        onClick = { onSelectTab(destination) },
                        text = { Text(destination.title) },
                    )
                }
            }
            if (currentUrl == null) {
                PortalUnavailable(
                    title = state.selectedTab.title,
                    message = when (state.selectedTab) {
                        PortalDestination.OPENCLASH -> "OpenClash URL is unavailable."
                        PortalDestination.ZASHBOARD -> "Zashboard is not installed or no URL is available."
                        PortalDestination.METACUBEXD -> "MetaCubeXD is not installed or no URL is available."
                    },
                )
            } else {
                PortalWebView(
                    url = currentUrl,
                    trustedHosts = state.trustedHosts,
                    onPageFinished = onSyncCookies,
                    onPageError = onSetPageError,
                    onTrustHost = onTrustHost,
                )
                state.pageError?.let { error ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }
    }

    if (state.showSettings) {
        SettingsDialog(
            state = state,
            onDismiss = onCloseSettings,
            onProtocolSelected = onProtocolSelected,
            onHostChanged = onHostChanged,
            onPortChanged = onPortChanged,
            onPasswordChanged = onPasswordChanged,
            onOpenClashUrlChanged = onOpenClashUrlChanged,
            onZashboardUrlChanged = onZashboardUrlChanged,
            onMetaCubeXdUrlChanged = onMetaCubeXdUrlChanged,
            onSave = onSaveSettings,
            onWipeAll = onWipeAll,
        )
    }
}

@Composable
private fun PortalUnavailable(
    title: String,
    message: String,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PortalWebView(
    url: String,
    trustedHosts: Set<String>,
    onPageFinished: (String) -> Unit,
    onPageError: (String?) -> Unit,
    onTrustHost: (String) -> Unit,
) {
    val context = LocalContext.current
    var pendingSslHandler by remember { mutableStateOf<SslErrorHandler?>(null) }
    var pendingSslHost by remember { mutableStateOf<String?>(null) }
    val currentTrustedHosts by rememberUpdatedState(trustedHosts)
    val currentOnPageFinished by rememberUpdatedState(onPageFinished)
    val currentOnPageError by rememberUpdatedState(onPageError)
    val currentOnTrustHost by rememberUpdatedState(onTrustHost)

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.destroy()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            webView.apply {
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                        super.onPageFinished(view, loadedUrl)
                        currentOnPageError(null)
                        loadedUrl?.let(currentOnPageFinished)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            currentOnPageError(error?.description?.toString() ?: "Page load failed")
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?,
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        if (request?.isForMainFrame == true) {
                            currentOnPageError("Page returned HTTP ${errorResponse?.statusCode ?: 0}")
                        }
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?,
                    ) {
                        val host = error?.url?.toHttpUrlOrNull()?.host
                        if (host != null && currentTrustedHosts.contains(host.lowercase())) {
                            handler?.proceed()
                            return
                        }
                        pendingSslHandler = handler
                        pendingSslHost = host
                    }
                }
                loadUrl(url)
            }
        },
        update = { currentWebView ->
            if (currentWebView.url != url) {
                currentWebView.loadUrl(url)
            }
        },
    )

    if (pendingSslHost != null) {
        AlertDialog(
            onDismissRequest = {
                pendingSslHandler?.cancel()
                pendingSslHandler = null
                pendingSslHost = null
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingSslHost?.let(currentOnTrustHost)
                    pendingSslHandler?.proceed()
                    pendingSslHandler = null
                    pendingSslHost = null
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingSslHandler?.cancel()
                    pendingSslHandler = null
                    pendingSslHost = null
                }) {
                    Text("Cancel")
                }
            },
            title = { Text("HTTPS certificate warning") },
            text = { Text("WebView reported an untrusted certificate for ${pendingSslHost.orEmpty()}. Continue loading this host?") },
        )
    }
}

@Composable
private fun SettingsDialog(
    state: MainUiState,
    onDismiss: () -> Unit,
    onProtocolSelected: (RouterProtocol) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onOpenClashUrlChanged: (String) -> Unit,
    onZashboardUrlChanged: (String) -> Unit,
    onMetaCubeXdUrlChanged: (String) -> Unit,
    onSave: () -> Unit,
    onWipeAll: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connection settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RouterProtocol.entries.forEach { protocol ->
                        FilterChip(
                            selected = state.protocol == protocol,
                            onClick = { onProtocolSelected(protocol) },
                            label = { Text(protocol.name) },
                        )
                    }
                }
                OutlinedTextField(
                    value = state.hostInput,
                    onValueChange = onHostChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Router host") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.portInput,
                    onValueChange = onPortChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("LuCI port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = state.passwordInput,
                    onValueChange = onPasswordChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("root password") },
                    singleLine = true,
                )
                HorizontalDivider()
                OutlinedTextField(
                    value = state.customOpenClashUrl,
                    onValueChange = onOpenClashUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Custom OpenClash URL") },
                )
                OutlinedTextField(
                    value = state.customZashboardUrl,
                    onValueChange = onZashboardUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Custom Zashboard URL") },
                )
                OutlinedTextField(
                    value = state.customMetaCubeXdUrl,
                    onValueChange = onMetaCubeXdUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Custom MetaCubeXD URL") },
                )
                OutlinedButton(onClick = onWipeAll, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear all local sessions and trusted hosts")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save and reconnect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
