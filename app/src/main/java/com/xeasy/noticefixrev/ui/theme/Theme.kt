package com.xeasy.noticefixrev.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Brand seed: a muted "salt" green-grey. Used on Android 11 and below, and as the
 * tonal fallback whenever dynamic color is unavailable.
 */
private val Salt40 = Color(0xFF3F6355)
private val Salt80 = Color(0xFFA5D0BC)
private val Salt10 = Color(0xFF002117)
private val Salt90 = Color(0xFFC1ECD8)
private val Salt20 = Color(0xFF0B3729)
private val Salt30 = Color(0xFF265141)

private val LightScheme = lightColorScheme(
    primary = Salt40,
    onPrimary = Color.White,
    primaryContainer = Salt90,
    onPrimaryContainer = Salt10,
    secondary = Color(0xFF4D6358),
    secondaryContainer = Color(0xFFD0E8DA),
    onSecondaryContainer = Color(0xFF0A1F17),
    tertiary = Color(0xFF456179),
    tertiaryContainer = Color(0xFFCCE5FF),
    background = Color(0xFFFBFDF9),
    surface = Color(0xFFFBFDF9),
    surfaceVariant = Color(0xFFDBE5DD),
    onSurfaceVariant = Color(0xFF3F4944),
)

private val DarkScheme = darkColorScheme(
    primary = Salt80,
    onPrimary = Salt20,
    primaryContainer = Salt30,
    onPrimaryContainer = Salt90,
    secondary = Color(0xFFB4CCBE),
    secondaryContainer = Color(0xFF354B40),
    onSecondaryContainer = Color(0xFFD0E8DA),
    tertiary = Color(0xFFACCAE5),
    tertiaryContainer = Color(0xFF2D4960),
    background = Color(0xFF191C1A),
    surface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFF3F4944),
    onSurfaceVariant = Color(0xFFBFC9C2),
)

@Composable
fun NoticeFixRevTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkScheme
        else -> LightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NoticeFixTypography,
        content = content,
    )
}
