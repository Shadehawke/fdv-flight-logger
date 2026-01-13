package com.fdv.fdvflightlogger.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import com.fdv.fdvflightlogger.data.prefs.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

// --- FDV Brand Schemes (Delta palette) ---

private val FdvLightColorScheme = lightColorScheme(
    primary = DeltaBlue,
    onPrimary = Color.White,
    secondary = DeltaRed,
    onSecondary = Color.White,

    // Use tertiary sparingly in UI (chips/icons, not large surfaces)
    tertiary = DeltaOrange,
    onTertiary = Color.Black,

    background = Color.White,
    onBackground = DeltaDarkBlue,
    surface = Color.White,
    onSurface = DeltaDarkBlue,
    surfaceVariant = Color(0xFFF2F4F7),
    onSurfaceVariant = DeltaDarkBlue,
    outline = Color(0xFFCBD5E1)
)

private val FdvDarkColorScheme = darkColorScheme(
    // Better contrast on dark surfaces than pure DeltaBlue
    primary = DeltaLightBlue,
    onPrimary = Color.Black,
    secondary = DeltaRed,
    onSecondary = Color.White,
    tertiary = DeltaOrange,
    onTertiary = Color.Black,

    background = Color(0xFF0B1220),
    onBackground = Color.White,
    surface = Color(0xFF0F1A2B),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF16243A),
    onSurfaceVariant = Color.White,
    outline = Color(0xFF334155)
)

@Composable
fun FDVFlightLoggerTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()

    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> {
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    dynamicLightColorScheme(LocalContext.current)
                else -> LightColorScheme
            }
        }

        ThemeMode.DARK -> {
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    dynamicDarkColorScheme(LocalContext.current)
                else -> DarkColorScheme
            }
        }

        ThemeMode.FDV -> {
            if (systemDark) FdvDarkColorScheme else FdvLightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
