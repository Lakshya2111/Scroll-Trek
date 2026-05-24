package com.example.scrolltrek.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    primaryContainer = PrimaryContainer,
    secondary = Accent,
    surface = Surface,
    onSurface = OnSurface,
    outline = Outline
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurface = PrimaryDark
)

private val AmoledColorScheme = darkColorScheme(
    primary = PrimaryDark,
    surface = SurfaceAmoled,
    surfaceVariant = SurfaceVariantAmoled,
    onSurface = PrimaryDark
)

@Composable
fun ScrollTrekTheme(
    themeSetting: String = "system", // "light" | "dark" | "amoled" | "system"
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeSetting) {
        "light" -> false
        "dark", "amoled" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when (themeSetting) {
        "light" -> LightColorScheme
        "dark" -> DarkColorScheme
        "amoled" -> AmoledColorScheme
        else -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scrollTrekTypography,
        shapes = scrollTrekShapes,
        content = content
    )
}