package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AppAdaptiveDimens(
    val isCompact: Boolean = false,
    val uiScaleMode: String = "Auto",
    val scaleFactor: Float = 1.0f,
    val horizontalPadding: Dp = 18.dp,
    val cardRadius: Dp = 20.dp,
    val itemSpacing: Dp = 14.dp,
    val headerIconSize: Dp = 40.dp,
    val avatarSizeSmall: Dp = 36.dp,
    val avatarSizeMedium: Dp = 44.dp,
    val avatarSizeLarge: Dp = 74.dp,
    val quickTileHeight: Dp = 74.dp,
    val buttonHeight: Dp = 50.dp,
    val bottomBarClearance: Dp = 104.dp
) {
    val cardCornerRadius: Dp get() = cardRadius
    val cardPadding: Dp get() = if (isCompact) 12.dp else 16.dp
    val fontScale: Float get() = scaleFactor
    val verticalSpacing: Dp get() = itemSpacing
    val topBarIconSize: Dp get() = headerIconSize
    val categoryIconSize: Dp get() = avatarSizeMedium
}

val LocalAppDimens = staticCompositionLocalOf { AppAdaptiveDimens() }
val LocalActivePalette = staticCompositionLocalOf { SplitBleePalettes.all.first() }

@Composable
fun SplitBleeTheme(
    themeName: String = "Ocean Blue",
    themeMode: String = "Light",
    uiScaleMode: String = "Auto",
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "Dark" -> true
        "System" -> isSystemInDarkTheme()
        else -> false
    }

    val palette = SplitBleePalettes.forName(themeName)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            primaryContainer = palette.deep,
            onPrimaryContainer = palette.lightContainer,
            secondary = palette.deep,
            onSecondary = Color.White,
            secondaryContainer = palette.darkSurface,
            onSecondaryContainer = palette.lightContainer,
            background = palette.darkBackground,
            onBackground = Color(0xFFF1F5F9),
            surface = palette.darkSurface,
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = palette.deep.copy(alpha = 0.55f),
            onSurfaceVariant = Color(0xFFCBD5E1),
            outline = palette.primary.copy(alpha = 0.35f)
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            primaryContainer = palette.lightContainer,
            onPrimaryContainer = palette.deep,
            secondary = palette.deep,
            onSecondary = Color.White,
            secondaryContainer = palette.lightCardTint,
            onSecondaryContainer = palette.deep,
            background = palette.lightBackground,
            onBackground = palette.deep,
            surface = palette.lightSurface,
            onSurface = palette.deep,
            surfaceVariant = palette.lightCardTint,
            onSurfaceVariant = palette.deep.copy(alpha = 0.72f),
            outline = palette.lightContainer
        )
    }

    val baseDensity = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthDp = maxWidth.value
        val heightDp = maxHeight.value

        val autoScale = when {
            widthDp <= 340f || heightDp <= 620f -> 0.82f
            widthDp <= 375f || heightDp <= 700f -> 0.89f
            widthDp <= 412f -> 0.96f
            widthDp >= 600f -> 1.06f
            else -> 1.0f
        }

        val effectiveScale = when (uiScaleMode) {
            "Compact" -> (autoScale * 0.86f).coerceIn(0.76f, 0.88f)
            "Standard" -> 1.0f
            "Expanded" -> 1.08f
            else -> autoScale
        }

        val isCompactActive = uiScaleMode == "Compact" || effectiveScale <= 0.90f

        val adaptiveDimens = AppAdaptiveDimens(
            isCompact = isCompactActive,
            uiScaleMode = uiScaleMode,
            scaleFactor = effectiveScale,
            horizontalPadding = (if (isCompactActive) 12f else 18f).dp,
            cardRadius = (if (isCompactActive) 16f else 22f).dp,
            itemSpacing = (if (isCompactActive) 10f else 15f).dp,
            headerIconSize = (if (isCompactActive) 36f else 42f).dp,
            avatarSizeSmall = (if (isCompactActive) 30f else 36f).dp,
            avatarSizeMedium = (if (isCompactActive) 38f else 44f).dp,
            avatarSizeLarge = (if (isCompactActive) 62f else 80f).dp,
            quickTileHeight = (if (isCompactActive) 62f else 76f).dp,
            buttonHeight = (if (isCompactActive) 44f else 52f).dp,
            bottomBarClearance = (if (isCompactActive) 86f else 108f).dp
        )

        val scaledDensity = Density(
            density = baseDensity.density * effectiveScale,
            fontScale = baseDensity.fontScale * effectiveScale.coerceIn(0.84f, 1.06f)
        )

        CompositionLocalProvider(
            LocalDensity provides scaledDensity,
            LocalAppDimens provides adaptiveDimens,
            LocalActivePalette provides palette
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                content = content
            )
        }
    }
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
