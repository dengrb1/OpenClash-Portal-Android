package com.openclash.portal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape

object PortalDimensions {
    val screenHorizontalPadding = 20.dp
    val contentSpacing = 16.dp
    val cardPadding = 20.dp
    val primaryActionHeight = 52.dp
    val iconTouchTarget = 48.dp
}

data class PortalStatusColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
)

private val LightStatusColors = PortalStatusColors(
    success = Color(0xFF1D6B45),
    onSuccess = Color(0xFFFFFFFF),
    warning = Color(0xFFFFE08A),
    onWarning = Color(0xFF382900),
)

private val DarkStatusColors = PortalStatusColors(
    success = Color(0xFF86D9A7),
    onSuccess = Color(0xFF00391C),
    warning = Color(0xFFFFD783),
    onWarning = Color(0xFF3F2E00),
)

val LocalPortalStatusColors = staticCompositionLocalOf { LightStatusColors }

private val LightScheme = lightColorScheme(
    primary = Color(0xFF006D77),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC4F2ED),
    onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFF365F65),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC6E8ED),
    onSecondaryContainer = Color(0xFF001F23),
    tertiary = Color(0xFF3D5E83),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD4E3FF),
    onTertiaryContainer = Color(0xFF001C38),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6FAFA),
    onBackground = Color(0xFF171D1D),
    surface = Color(0xFFF6FAFA),
    onSurface = Color(0xFF171D1D),
    surfaceVariant = Color(0xFFDAE5E4),
    onSurfaceVariant = Color(0xFF3F4948),
    outline = Color(0xFF6F7978),
    outlineVariant = Color(0xFFBEC9C8),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF78D5CD),
    onPrimary = Color(0xFF003738),
    primaryContainer = Color(0xFF005052),
    onPrimaryContainer = Color(0xFF94F2E9),
    secondary = Color(0xFFAACCD0),
    onSecondary = Color(0xFF0B3439),
    secondaryContainer = Color(0xFF244B51),
    onSecondaryContainer = Color(0xFFC6E8ED),
    tertiary = Color(0xFFB7C8EA),
    onTertiary = Color(0xFF263F61),
    tertiaryContainer = Color(0xFF3D5679),
    onTertiaryContainer = Color(0xFFD4E3FF),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1515),
    onBackground = Color(0xFFDEE4E3),
    surface = Color(0xFF0E1515),
    onSurface = Color(0xFFDEE4E3),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBEC9C8),
    outline = Color(0xFF899392),
    outlineVariant = Color(0xFF3F4948),
)

private val PortalShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val PortalTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)

@Composable
fun OpenClashPortalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = PortalTypography,
        shapes = PortalShapes,
    ) {
        CompositionLocalProvider(LocalPortalStatusColors provides statusColors, content = content)
    }
}
