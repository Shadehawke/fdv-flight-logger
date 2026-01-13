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

    // Limited tertiary usage per brand guidance; keep it available but not dominant
    tertiary = DeltaLightBlue,
    onTertiary = Color.White,

    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF0E1A2B),

    surface = Color.White,
    onSurface = Color(0xFF0E1A2B),

    surfaceVariant = Color(0xFFE6ECF3),
    onSurfaceVariant = Color(0xFF23364D),

    outline = Color(0xFF8AA0B8),
    outlineVariant = Color(0xFFC9D6E4),

    error = Color(0xFFB3261E),
    onError = Color.White
)

private val FdvDarkColorScheme = darkColorScheme(
    // In dark mode, DeltaBlue as "primary" can be too deep; use lighter blue for accents.
    primary = DeltaLightBlue,
    onPrimary = Color(0xFF07121F),

    secondary = DeltaRed,
    onSecondary = Color.White,

    tertiary = DeltaGreen,
    onTertiary = Color(0xFF07121F),

    background = Color(0xFF0B1422),
    onBackground = Color(0xFFEAF0F8),

    surface = Color(0xFF101E33),
    onSurface = Color(0xFFEAF0F8),

    surfaceVariant = Color(0xFF162844),
    onSurfaceVariant = Color(0xFFC9D6E4),

    outline = Color(0xFF2C4568),
    outlineVariant = Color(0xFF23364D),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)


@Composable
fun FDVFlightLoggerTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()

    val colorScheme = when (themeMode) {

        ThemeMode.FDV -> {
            // Brand palette must not be overridden by dynamic color
            if (systemDark) FdvDarkColorScheme else FdvLightColorScheme
        }

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
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
