package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Approved Ocean Blue Mockup Palette
val OceanBluePrimary = Color(0xFF1D5CE5)
val OceanBlueDeepNavy = Color(0xFF102A63)
val OceanBlueDarkText = Color(0xFF0B1E40)
val OceanBlueSecondaryText = Color(0xFF566A8E)
val OceanBlueBackground = Color(0xFFEEF4FC)
val OceanBlueSurface = Color(0xFFFFFFFF)
val OceanBlueCardTint = Color(0xFFF6F9FE)
val OceanBlueDivider = Color(0xFFDCE7F7)

// Status Pill Colors matching Mockup
val StatusPaidBg = Color(0xFFDDF6E8)
val StatusPaidText = Color(0xFF15803D)
val StatusUnpaidBg = Color(0xFFFDE8E8)
val StatusUnpaidText = Color(0xFFC81E1E)
val StatusPartialBg = Color(0xFFFEF3C7)
val StatusPartialText = Color(0xFFB45309)

data class SplitBleeThemePalette(
    val name: String,
    val isPastel: Boolean,
    val primary: Color,
    val deep: Color,
    val lightBackground: Color,
    val lightSurface: Color,
    val lightCardTint: Color,
    val lightContainer: Color,
    val darkBackground: Color,
    val darkSurface: Color,
    val previewSwatches: List<Color>
) {
    val accentChipBg: Color get() = lightContainer
    val deepNavy: Color get() = deep
}

object SplitBleePalettes {
    val all: List<SplitBleeThemePalette> = listOf(
        SplitBleeThemePalette(
            name = "Ocean Blue",
            isPastel = false,
            primary = Color(0xFF1D5CE5),
            deep = Color(0xFF102A63),
            lightBackground = Color(0xFFEEF4FC),
            lightSurface = Color(0xFFFFFFFF),
            lightCardTint = Color(0xFFF5F9FF),
            lightContainer = Color(0xFFDCE8FC),
            darkBackground = Color(0xFF0A1428),
            darkSurface = Color(0xFF132240),
            previewSwatches = listOf(Color(0xFF1D5CE5), Color(0xFF102A63), Color(0xFFDCE8FC))
        ),
        SplitBleeThemePalette(
            name = "Pastel Sky",
            isPastel = true,
            primary = Color(0xFF4A8FE7),
            deep = Color(0xFF1E4E8C),
            lightBackground = Color(0xFFF2F8FF),
            lightSurface = Color(0xFFFFFFFF),
            lightCardTint = Color(0xFFEAF3FF),
            lightContainer = Color(0xFFD4E8FF),
            darkBackground = Color(0xFF0E1B2E),
            darkSurface = Color(0xFF172A45),
            previewSwatches = listOf(Color(0xFF7BB3F7), Color(0xFF4A8FE7), Color(0xFFD4E8FF))
        ),
        SplitBleeThemePalette(
            name = "Pastel Mint",
            isPastel = true,
            primary = Color(0xFF2FA88F),
            deep = Color(0xFF165B4C),
            lightBackground = Color(0xFFF1FBF8),
            lightSurface = Color(0xFFFFFFFF),
            lightCardTint = Color(0xFFE6F7F2),
            lightContainer = Color(0xFFC9F0E5),
            darkBackground = Color(0xFF0B211D),
            darkSurface = Color(0xFF13332C),
            previewSwatches = listOf(Color(0xFF6DD3BC), Color(0xFF2FA88F), Color(0xFFC9F0E5))
        ),
        SplitBleeThemePalette(
            name = "Pastel Lavender",
            isPastel = true,
            primary = Color(0xFF7C6EE6),
            deep = Color(0xFF3E328F),
            lightBackground = Color(0xFFF6F4FF),
            lightSurface = Color(0xFFFFFFFF),
            lightCardTint = Color(0xFFEEEBFF),
            lightContainer = Color(0xFFDFD9FF),
            darkBackground = Color(0xFF14112B),
            darkSurface = Color(0xFF211C42),
            previewSwatches = listOf(Color(0xFFA599F7), Color(0xFF7C6EE6), Color(0xFFDFD9FF))
        ),
        SplitBleeThemePalette(
            name = "Pastel Peach",
            isPastel = true,
            primary = Color(0xFFE06D53),
            deep = Color(0xFF7C3222),
            lightBackground = Color(0xFFFFF6F3),
            lightSurface = Color(0xFFFFFFFF),
            lightCardTint = Color(0xFFFFECE6),
            lightContainer = Color(0xFFFFD8CC),
            darkBackground = Color(0xFF26120E),
            darkSurface = Color(0xFF3B1E18),
            previewSwatches = listOf(Color(0xFFF79D84), Color(0xFFE06D53), Color(0xFFFFD8CC))
        ),
        SplitBleeThemePalette(
            name = "Pastel Rose",
            isPastel = true,
            primary = Color(0xFFDB5A8B),
            deep = Color(0xFF7A2447),
            lightBackground = Color(0xFFFFF4F8),
            lightSurface = Color(0xFFFFFFFF),
            lightCardTint = Color(0xFFFFEBF2),
            lightContainer = Color(0xFFFFD4E4),
            darkBackground = Color(0xFF260F1A),
            darkSurface = Color(0xFF3B1929),
            previewSwatches = listOf(Color(0xFFF48FB1), Color(0xFFDB5A8B), Color(0xFFFFD4E4))
        ),
        SplitBleeThemePalette(
            name = "Pastel Lemon",
            isPastel = true,
            primary = Color(0xFFC98A1C),
            deep = Color(0xFF6B4608),
            lightBackground = Color(0xFFFFFCF0),
            lightSurface = Color(0xFFFFFFFF),
            lightCardTint = Color(0xFFFFF7DB),
            lightContainer = Color(0xFFFDECB2),
            darkBackground = Color(0xFF241B08),
            darkSurface = Color(0xFF382A0E),
            previewSwatches = listOf(Color(0xFFF6C453), Color(0xFFC98A1C), Color(0xFFFDECB2))
        ),
        SplitBleeThemePalette(
            name = "Pastel Lilac",
            isPastel = true,
            primary = Color(0xFFA855C7),
            deep = Color(0xFF58216E),
            lightBackground = Color(0xFFFCF4FF),
            lightSurface = Color(0xFFFFFFFF),
            lightCardTint = Color(0xFFF6E8FC),
            lightContainer = Color(0xFFEBD0F7),
            darkBackground = Color(0xFF210E29),
            darkSurface = Color(0xFF351842),
            previewSwatches = listOf(Color(0xFFCE8CE8), Color(0xFFA855C7), Color(0xFFEBD0F7))
        ),
        SplitBleeThemePalette(
            name = "Mint",
            isPastel = false,
            primary = Color(0xFF0D9488),
            deep = Color(0xFF134E4A),
            lightBackground = Color(0xFFF0FDFA),
            lightSurface = Color(0xFFFFFFFF),
            lightCardTint = Color(0xFFE6FAF5),
            lightContainer = Color(0xFFCCFBF1),
            darkBackground = Color(0xFF08201E),
            darkSurface = Color(0xFF113532),
            previewSwatches = listOf(Color(0xFF0D9488), Color(0xFF134E4A), Color(0xFFCCFBF1))
        ),
        SplitBleeThemePalette(
            name = "Lavender",
            isPastel = false,
            primary = Color(0xFF6366F1),
            deep = Color(0xFF312E81),
            lightBackground = Color(0xFFF5F3FF),
            lightSurface = Color(0xFFFFFFFF),
            lightCardTint = Color(0xFFEDE9FE),
            lightContainer = Color(0xFFE0E7FF),
            darkBackground = Color(0xFF0F1029),
            darkSurface = Color(0xFF1B1D47),
            previewSwatches = listOf(Color(0xFF6366F1), Color(0xFF312E81), Color(0xFFE0E7FF))
        ),
        SplitBleeThemePalette(
            name = "Sand",
            isPastel = false,
            primary = Color(0xFFD97706),
            deep = Color(0xFF78350F),
            lightBackground = Color(0xFFFFFBEB),
            lightSurface = Color(0xFFFFFFFF),
            lightCardTint = Color(0xFFFEF3C7),
            lightContainer = Color(0xFFFDE68A),
            darkBackground = Color(0xFF211507),
            darkSurface = Color(0xFF36220B),
            previewSwatches = listOf(Color(0xFFD97706), Color(0xFF78350F), Color(0xFFFDE68A))
        ),
        SplitBleeThemePalette(
            name = "Graphite",
            isPastel = false,
            primary = Color(0xFF334155),
            deep = Color(0xFF0F172A),
            lightBackground = Color(0xFFF1F5F9),
            lightSurface = Color(0xFFFFFFFF),
            lightCardTint = Color(0xFFE2E8F0),
            lightContainer = Color(0xFFCBD5E1),
            darkBackground = Color(0xFF090D16),
            darkSurface = Color(0xFF151E2E),
            previewSwatches = listOf(Color(0xFF334155), Color(0xFF0F172A), Color(0xFFCBD5E1))
        ),
        SplitBleeThemePalette(
            name = "Rose",
            isPastel = false,
            primary = Color(0xFFE11D48),
            deep = Color(0xFF881337),
            lightBackground = Color(0xFFFFF1F2),
            lightSurface = Color(0xFFFFFFFF),
            lightCardTint = Color(0xFFFFE4E6),
            lightContainer = Color(0xFFFECDD3),
            darkBackground = Color(0xFF240911),
            darkSurface = Color(0xFF3D101E),
            previewSwatches = listOf(Color(0xFFE11D48), Color(0xFF881337), Color(0xFFFECDD3))
        )
    )

    fun forName(name: String): SplitBleeThemePalette {
        return all.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: all.first()
    }
}
