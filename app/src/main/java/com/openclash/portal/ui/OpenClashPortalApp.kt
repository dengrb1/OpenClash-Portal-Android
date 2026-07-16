@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.openclash.portal.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.openclash.portal.R
import com.openclash.portal.model.AppLanguage
import com.openclash.portal.model.PortalDestination
import com.openclash.portal.model.RouterProtocol
import com.openclash.portal.ui.theme.PortalDimensions
import com.openclash.portal.ui.theme.LocalPortalStatusColors
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
                    Text(stringResource(R.string.trust_and_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissSslPrompt) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.untrusted_https_certificate)) },
            text = { Text(stringResource(R.string.allow_insecure_https_for_host, state.pendingSslHost.orEmpty())) },
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
            onLanguageSelected = viewModel::onLanguageSelected,
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
            onLanguageSelected = viewModel::onLanguageSelected,
        )
    }
}

@Composable
internal fun ConnectionScreen(
    state: MainUiState,
    onProtocolSelected: (RouterProtocol) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConnect: () -> Unit,
    onDiscover: () -> Unit,
    onCandidateSelected: (String) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PortalDimensions.screenHorizontalPadding, vertical = PortalDimensions.contentSpacing)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            WelcomeHeader()
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(PortalDimensions.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.connect_to_router),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(R.string.connection_details),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.protocol),
                        style = MaterialTheme.typography.titleSmall,
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
                    OutlinedTextField(
                        value = state.hostInput,
                        onValueChange = onHostChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.router_host_or_url)) },
                        leadingIcon = { Icon(Icons.Filled.Router, null) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.portInput,
                        onValueChange = onPortChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.luci_port)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = state.passwordInput,
                        onValueChange = onPasswordChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.openwrt_root_password)) },
                        singleLine = true,
                    )
                    RouterDiscovery(
                        state = state,
                        onDiscover = onDiscover,
                        onCandidateSelected = onCandidateSelected,
                    )
                    ConnectionStatusCard(state = state)
                    Button(
                        onClick = onConnect,
                        enabled = !state.isConnecting && !state.isInitializing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(PortalDimensions.primaryActionHeight)
                            .testTag("connect-button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        if (state.isConnecting || state.isInitializing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            if (state.isConnecting || state.isInitializing) {
                                stringResource(R.string.connecting)
                            } else {
                                stringResource(R.string.connect_and_open_openclash)
                            },
                        )
                    }
                }
            }
            LanguageSelector(
                selectedLanguage = state.appLanguage,
                onLanguageSelected = onLanguageSelected,
            )
        }
    }
}

@Composable
private fun WelcomeHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                imageVector = Icons.Filled.Router,
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.padding(14.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.connection_welcome_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.connection_welcome_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RouterDiscovery(
    state: MainUiState,
    onDiscover: () -> Unit,
    onCandidateSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.discovered_router_addresses),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onDiscover,
                enabled = !state.isDiscovering,
                modifier = Modifier.testTag("discover-button"),
            ) {
                if (state.isDiscovering) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.discover))
                }
            }
        }
        if (state.isDiscovering) {
            Text(
                text = stringResource(R.string.finding_routers),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.discoveryCandidates.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.discoveryCandidates.forEach { candidate ->
                    AssistChip(
                        onClick = { onCandidateSelected(candidate) },
                        label = { Text(candidate) },
                        leadingIcon = { Icon(Icons.Filled.Router, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(state: MainUiState) {
    val loading = state.isConnecting || state.isInitializing
    val error = state.connectionError
    if (!loading && error == null) return

    val isError = error != null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (isError) "connection-error" else "connection-progress"),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else LocalPortalStatusColors.current.warning,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isError) {
                Icon(
                    Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = LocalPortalStatusColors.current.onWarning,
                    strokeWidth = 2.dp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isError) stringResource(R.string.connection_failed) else stringResource(R.string.connection_in_progress),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isError) MaterialTheme.colorScheme.onErrorContainer else LocalPortalStatusColors.current.onWarning,
                )
                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PortalScreen(
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
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    val currentUrl = state.resolvedUrls?.urlFor(state.selectedTab)
    val openExternally = state.selectedTab == PortalDestination.ZASHBOARD || state.selectedTab == PortalDestination.METACUBEXD
    Scaffold(
        topBar = {
            PortalTopBar(
                host = state.activeProfile?.normalizedHost ?: stringResource(R.string.app_name),
                onReconnect = onReconnect,
                onOpenSettings = onOpenSettings,
                onLogout = onLogout,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PortalNavigation(
                selectedDestination = state.selectedTab,
                onSelectTab = onSelectTab,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (currentUrl == null) {
                    PortalUnavailable(
                        title = state.selectedTab.displayName(),
                        message = when (state.selectedTab) {
                            PortalDestination.OPENCLASH -> stringResource(R.string.openclash_unavailable)
                            PortalDestination.ZASHBOARD -> stringResource(R.string.zashboard_unavailable)
                            PortalDestination.METACUBEXD -> stringResource(R.string.metacubexd_unavailable)
                        },
                        onRetry = onReconnect,
                    )
                } else if (openExternally) {
                    ExternalPanelScreen(
                        url = currentUrl,
                        panelName = state.selectedTab.displayName(),
                        onReturnToOpenClash = { onSelectTab(PortalDestination.OPENCLASH) },
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
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .testTag("page-error"),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.page_error),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
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
            onLanguageSelected = onLanguageSelected,
        )
    }
}

@Composable
private fun PortalTopBar(
    host: String,
    onReconnect: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Router,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(host, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = LocalPortalStatusColors.current.success,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.connected),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onReconnect) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.reconnect))
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.logout))
            }
        },
    )
}

@Composable
internal fun PortalNavigation(
    selectedDestination: PortalDestination,
    onSelectTab: (PortalDestination) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("portal-navigation"),
    ) {
        PortalDestination.entries.forEachIndexed { index, destination ->
            SegmentedButton(
                selected = selectedDestination == destination,
                onClick = { onSelectTab(destination) },
                shape = SegmentedButtonDefaults.itemShape(index, PortalDestination.entries.size),
                modifier = Modifier.testTag("portal-tab-${destination.name.lowercase()}"),
                icon = {
                    Icon(
                        imageVector = destination.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                label = {
                    Text(
                        text = destination.displayName(),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@Composable
private fun ExternalPanelScreen(
    url: String,
    panelName: String,
    onReturnToOpenClash: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val returnController = remember(url) { ExternalBrowserReturnController() }
    var launchRequested by remember(url) { mutableStateOf(false) }
    var launchError by remember(url) { mutableStateOf<String?>(null) }
    val browserLaunchFailedMessage = context.getString(R.string.browser_launch_failed)

    fun launchPanel() {
        if (launchRequested) return
        launchError = null
        try {
            val uri = Uri.parse(url)
            CustomTabsIntent.Builder().build().launchUrl(context, uri)
        } catch (_: Throwable) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (_: Throwable) {
                launchError = browserLaunchFailedMessage
            }
        }
        if (launchError == null) {
            returnController.markBrowserLaunched()
            launchRequested = true
        }
    }

    LaunchedEffect(url) {
        if (!launchRequested) {
            launchPanel()
        }
    }

    DisposableEffect(lifecycleOwner, returnController) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> returnController.markAppBackgrounded()
                Lifecycle.Event.ON_RESUME -> if (returnController.consumeReturn()) {
                    launchRequested = false
                    onReturnToOpenClash()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        IllustratedState(
            icon = Icons.AutoMirrored.Filled.OpenInNew,
            title = panelName,
            message = stringResource(R.string.browser_panel_ready),
            actionLabel = stringResource(R.string.open_panel),
            onAction = ::launchPanel,
            actionEnabled = !launchRequested,
            error = launchError,
        )
    }
}

@Composable
private fun PortalUnavailable(
    title: String,
    message: String,
    onRetry: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        IllustratedState(
            icon = Icons.Filled.WifiOff,
            title = title,
            message = message,
            actionLabel = stringResource(R.string.reconnect),
            actionIcon = Icons.Filled.Refresh,
            onAction = onRetry,
        )
    }
}

@Composable
private fun IllustratedState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionLabel: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.AutoMirrored.Filled.OpenInNew,
    onAction: () -> Unit,
    actionEnabled: Boolean = true,
    error: String? = null,
) {
    Column(
        modifier = Modifier.padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onAction, enabled = actionEnabled) {
            Icon(actionIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(actionLabel)
        }
        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
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
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val currentTrustedHosts by rememberUpdatedState(trustedHosts)
    val currentOnPageFinished by rememberUpdatedState(onPageFinished)
    val currentOnPageError by rememberUpdatedState(onPageError)
    val currentOnTrustHost by rememberUpdatedState(onTrustHost)
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        fileChooserCallback?.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data),
        )
        fileChooserCallback = null
    }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            fileChooserCallback?.onReceiveValue(null)
            fileChooserCallback = null
            webView.destroy()
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .testTag("portal-webview"),
        factory = {
            webView.apply {
                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: WebChromeClient.FileChooserParams?,
                    ): Boolean {
                        val callback = filePathCallback ?: return false
                        fileChooserCallback?.onReceiveValue(null)
                        fileChooserCallback = callback

                        val chooserIntent = runCatching { fileChooserParams?.createIntent() }.getOrNull()
                        if (chooserIntent == null) {
                            fileChooserCallback = null
                            callback.onReceiveValue(null)
                            return false
                        }

                        return runCatching {
                            fileChooserLauncher.launch(chooserIntent)
                            true
                        }.getOrElse {
                            fileChooserCallback = null
                            callback.onReceiveValue(null)
                            false
                        }
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                        super.onPageFinished(view, loadedUrl)
                        view?.installOpenClashModalCompatibilityStyles()
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
                            currentOnPageError(error?.description?.toString() ?: context.getString(R.string.page_load_failed))
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?,
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        if (request?.isForMainFrame == true) {
                            currentOnPageError(context.getString(R.string.page_returned_http, errorResponse?.statusCode ?: 0))
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
                    Text(stringResource(R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingSslHandler?.cancel()
                    pendingSslHandler = null
                    pendingSslHost = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.https_certificate_warning)) },
            text = { Text(stringResource(R.string.webview_untrusted_certificate, pendingSslHost.orEmpty())) },
        )
    }
}

private fun WebView.installOpenClashModalCompatibilityStyles() {
    evaluateJavascript(OPENCLASH_MODAL_COMPATIBILITY_SCRIPT, null)
}

/**
 * OpenClash renders several actions as custom fixed overlays instead of native dialogs. Some
 * Android WebView implementations paint their backdrop but lose the centered card when the
 * overlay uses backdrop-filter. Keep all OpenClash-style modal cards within the visual viewport
 * and use an opaque backdrop that is reliable across WebView versions.
 */
internal val OPENCLASH_MODAL_COMPATIBILITY_SCRIPT = """
    (function() {
        var styleId = 'openclash-portal-modal-compatibility';
        if (document.getElementById(styleId)) return;

        var style = document.createElement('style');
        style.id = styleId;
        style.textContent = `
            #config-upload-overlay.config-upload-model-overlay.show,
            .modal-overlay.show,
            .modal-backdrop.show,
            [class*="-modal-overlay"].show,
            [class*="-model-overlay"].show {
                position: fixed !important;
                inset: 0 !important;
                display: flex !important;
                align-items: center !important;
                justify-content: center !important;
                width: 100vw !important;
                height: 100vh !important;
                box-sizing: border-box !important;
                padding: 16px !important;
                overflow: auto !important;
                background: rgba(0, 0, 0, 0.5) !important;
                -webkit-backdrop-filter: none !important;
                backdrop-filter: none !important;
            }

            #config-upload-overlay.config-upload-model-overlay.show > #config-upload-model.config-upload-model,
            .modal-overlay.show > .modal,
            .modal-backdrop.show > .modal,
            [class*="-modal-overlay"].show > [class*="-modal"],
            [class*="-model-overlay"].show > [class*="-model"] {
                display: flex !important;
                width: 100% !important;
                max-width: 550px !important;
                max-height: calc(100vh - 32px) !important;
                min-height: 0 !important;
                box-sizing: border-box !important;
                margin: auto !important;
                overflow-y: auto !important;
                flex-shrink: 0 !important;
                position: relative !important;
                z-index: 1 !important;
            }
        `;
        (document.head || document.documentElement).appendChild(style);
    })();
""".trimIndent()

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
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.connection_settings)) },
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
                    label = { Text(stringResource(R.string.router_host)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.portInput,
                    onValueChange = onPortChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.luci_port)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = state.passwordInput,
                    onValueChange = onPasswordChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.root_password)) },
                    singleLine = true,
                )
                HorizontalDivider()
                OutlinedTextField(
                    value = state.customOpenClashUrl,
                    onValueChange = onOpenClashUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.custom_openclash_url)) },
                )
                OutlinedTextField(
                    value = state.customZashboardUrl,
                    onValueChange = onZashboardUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.custom_zashboard_url)) },
                )
                OutlinedTextField(
                    value = state.customMetaCubeXdUrl,
                    onValueChange = onMetaCubeXdUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.custom_metacubexd_url)) },
                )
                LanguageSelector(
                    selectedLanguage = state.appLanguage,
                    onLanguageSelected = onLanguageSelected,
                )
                OutlinedButton(onClick = onWipeAll, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.clear_all_sessions))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.save_and_reconnect))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}


@Composable
private fun PortalDestination.displayName(): String {
    return when (this) {
        PortalDestination.OPENCLASH -> stringResource(R.string.tab_openclash)
        PortalDestination.ZASHBOARD -> stringResource(R.string.tab_zashboard)
        PortalDestination.METACUBEXD -> stringResource(R.string.tab_metacubexd)
    }
}

private fun PortalDestination.icon(): androidx.compose.ui.graphics.vector.ImageVector {
    return when (this) {
        PortalDestination.OPENCLASH -> Icons.Filled.Router
        PortalDestination.ZASHBOARD -> Icons.AutoMirrored.Filled.OpenInNew
        PortalDestination.METACUBEXD -> Icons.Filled.Language
    }
}


@Composable
private fun LanguageSelector(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.language),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppLanguage.entries.forEach { language ->
                FilterChip(
                    selected = selectedLanguage == language,
                    onClick = { onLanguageSelected(language) },
                    label = {
                        Text(
                            when (language) {
                                AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                                AppLanguage.SIMPLIFIED_CHINESE -> stringResource(R.string.language_simplified_chinese)
                            },
                        )
                    },
                )
            }
        }
    }
}
