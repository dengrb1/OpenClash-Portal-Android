package com.openclash.portal.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.openclash.portal.model.PortalDestination
import com.openclash.portal.model.RouterProtocol
import com.openclash.portal.ui.theme.OpenClashPortalTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PortalUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loginPrimaryActionIsAvailableInLightAndDarkThemes() {
        var darkTheme by mutableStateOf(false)
        composeRule.setContent {
            OpenClashPortalTheme(darkTheme = darkTheme) {
                ConnectionScreen(
                    state = MainUiState(isInitializing = false),
                    onProtocolSelected = {},
                    onHostChanged = {},
                    onPortChanged = {},
                    onPasswordChanged = {},
                    onConnect = {},
                    onDiscover = {},
                    onCandidateSelected = {},
                    onLanguageSelected = {},
                )
            }
        }

        composeRule.onNodeWithTag("connect-button").assertIsDisplayed().assertIsEnabled()
        composeRule.runOnIdle { darkTheme = true }
        composeRule.onNodeWithTag("connect-button").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun loginShowsLoadingErrorAndDiscoveryResults() {
        var screenState by mutableStateOf(
            MainUiState(
                isInitializing = false,
                isConnecting = true,
                discoveryCandidates = listOf("192.168.1.1"),
            ),
        )
        var selectedCandidate: String? = null
        composeRule.setContent {
            OpenClashPortalTheme {
                ConnectionScreen(
                    state = screenState,
                    onProtocolSelected = {},
                    onHostChanged = {},
                    onPortChanged = {},
                    onPasswordChanged = {},
                    onConnect = {},
                    onDiscover = {},
                    onCandidateSelected = { selectedCandidate = it },
                    onLanguageSelected = {},
                )
            }
        }

        composeRule.onNodeWithTag("connection-progress").assertIsDisplayed()
        composeRule.onNodeWithText("192.168.1.1").performClick()
        composeRule.runOnIdle {
            check(selectedCandidate == "192.168.1.1")
            screenState = screenState.copy(isConnecting = false, connectionError = "Network unavailable")
        }
        composeRule.onNodeWithTag("connection-error").assertIsDisplayed()
    }

    @Test
    fun managementSegmentedNavigationSelectsEachDestination() {
        var selectedDestination by mutableStateOf(PortalDestination.OPENCLASH)
        composeRule.setContent {
            OpenClashPortalTheme {
                PortalNavigation(
                    selectedDestination = selectedDestination,
                    onSelectTab = { selectedDestination = it },
                )
            }
        }

        composeRule.onNodeWithTag("portal-tab-zashboard").performClick()
        composeRule.runOnIdle { check(selectedDestination == PortalDestination.ZASHBOARD) }
        composeRule.onNodeWithTag("portal-tab-metacubexd").performClick()
        composeRule.runOnIdle { check(selectedDestination == PortalDestination.METACUBEXD) }
    }
}
