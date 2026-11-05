package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SplitBleeTheme(
    themeName: String = "Ocean Blue",
    themeMode: String = "Light",
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "Dark" -> true
        "System" -> isSystemInDarkTheme()
        else -> false
    }

    val (primary, deep, bg) = when (themeName) {
        "Mint" -> Triple(MintPrimary, MintDeep, MintBackground)
        "Lavender" -> Triple(LavenderPrimary, LavenderDeep, LavenderBackground)
        "Sand" -> Triple(SandPrimary, SandDeep, SandBackground)
        "Graphite" -> Triple(GraphitePrimary, GraphiteDeep, GraphiteBackground)
        "Rose" -> Triple(RosePrimary, RoseDeep, RoseBackground)
        else -> Triple(OceanBluePrimary, OceanBlueDeepNavy, OceanBlueBackground)
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = deep,
            onPrimaryContainer = Color(0xFFD8E6FF),
            secondary = primary,
            onSecondary = Color.White,
            background = Color(0xFF0A1428),
            onBackground = Color(0xFFEAF1FC),
            surface = Color(0xFF132240),
            onSurface = Color(0xFFEAF1FC),
            surfaceVariant = Color(0xFF1B2F54),
            onSurfaceVariant = Color(0xFF9EB3D6),
            outline = Color(0xFF2B426E)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDCE8FC),
            onPrimaryContainer = deep,
            secondary = deep,
            onSecondary = Color.White,
            background = bg,
            onBackground = OceanBlueDarkText,
            surface = OceanBlueSurface,
            onSurface = OceanBlueDarkText,
            surfaceVariant = OceanBlueCardTint,
            onSurfaceVariant = OceanBlueSecondaryText,
            outline = OceanBlueDivider
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    SplitBleeTheme(
        themeName = "Ocean Blue",
        themeMode = if (darkTheme) "Dark" else "Light",
        content = content
    )
}
