package com.openclash.portal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme()

@Composable
fun OpenClashPortalTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightScheme,
        content = content,
    )
}
