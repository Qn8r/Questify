package com.example.livinglifemmo

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

/* ===========================
   THEME ENGINE
   =========================== */

object ThemeEngine {
    fun getColors(theme: AppTheme): Pair<Color, Color> {
        return when (theme) {
            AppTheme.DEFAULT -> AccentBurntOrange to DarkBackground
            AppTheme.LIGHT -> AccentBurntOrange to Color(0xFFF6F8FC)
            AppTheme.CYBERPUNK -> {
                if (ThemeRuntime.neonLightBoostEnabled) {
                    Color(0xFF3FFFE8) to Color(0xFF0D1024)
                } else {
                    Color(0xFF00F5D4) to Color(0xFF090B1A)
                }
            }
        }
    }
}

fun AppTheme.isLightCategory(): Boolean {
    return this == AppTheme.LIGHT
}

object ThemeRuntime {
    var currentTheme by mutableStateOf(AppTheme.DEFAULT)
    var highContrastTextEnabled by mutableStateOf(false)
    var reduceAnimationsEnabled by mutableStateOf(false)
    var compactModeEnabled by mutableStateOf(false)
    var largerTouchTargetsEnabled by mutableStateOf(false)
    var decorativeBordersEnabled by mutableStateOf(false)
    var neonLightBoostEnabled by mutableStateOf(false)
    var neonFlowEnabled by mutableStateOf(true)
    var neonFlowSpeed by mutableIntStateOf(0)
    var neonGlowPalette by mutableStateOf("magenta")
    var fontStyle by mutableStateOf(AppFontStyle.DEFAULT)
    var fontScalePercent by mutableIntStateOf(100)
    var textColorOverride by mutableStateOf<Color?>(null)
    var cardColorOverride by mutableStateOf<Color?>(null)
    var accentTransparencyPercent by mutableIntStateOf(0)
    var textTransparencyPercent by mutableIntStateOf(0)
    var appBgTransparencyPercent by mutableIntStateOf(0)
    var chromeBgTransparencyPercent by mutableIntStateOf(0)
    var cardBgTransparencyPercent by mutableIntStateOf(0)
    var journalPageTransparencyPercent by mutableIntStateOf(0)
    var journalAccentTransparencyPercent by mutableIntStateOf(0)
    var buttonTransparencyPercent by mutableIntStateOf(0)
    var settingsThemeLabExpanded by mutableStateOf(false)
    var settingsThemePresetIndex by mutableIntStateOf(-1)
}

fun neonPaletteColor(key: String, boosted: Boolean): Color {
    return when (key.lowercase()) {
        "cyan" -> if (boosted) Color(0xFF7AFFF2) else Color(0xFF35EEDB)
        "violet" -> if (boosted) Color(0xFFBAA5FF) else Color(0xFF8B6DFF)
        "sunset" -> if (boosted) Color(0xFFFFB27A) else Color(0xFFFF8A4D)
        "lime" -> if (boosted) Color(0xFFC7FF78) else Color(0xFF9DFF4D)
        "ice" -> if (boosted) Color(0xFFA5E6FF) else Color(0xFF66CCFF)
        else -> if (boosted) Color(0xFFFF72C5) else Color(0xFFFF2E97)
    }
}

/* ===========================
   COLORS
   =========================== */

val AccentYellow      = Color(0xFFFFD54A)
val AccentBurntOrange = Color(0xFF4C8ED9)
val DarkBackground    = Color(0xFF0C1118)

private fun applyTransparencyPercent(color: Color, transparencyPercent: Int): Color {
    val alpha = (1f - (transparencyPercent.coerceIn(0, 100) / 100f)).coerceIn(0f, 1f)
    return color.copy(alpha = alpha)
}

fun Color.scaleAlpha(multiplier: Float): Color {
    val m = multiplier.coerceIn(0f, 1f)
    return copy(alpha = (alpha * m).coerceIn(0f, 1f))
}

val CardDarkBlue: Color
    get() = applyTransparencyPercent(
        when {
        ThemeRuntime.cardColorOverride != null -> ThemeRuntime.cardColorOverride!!
        ThemeRuntime.currentTheme.isLightCategory() -> Color(0xFFFFFFFF)
        ThemeRuntime.currentTheme == AppTheme.CYBERPUNK && ThemeRuntime.neonLightBoostEnabled -> Color(0xFF19204A)
        ThemeRuntime.currentTheme == AppTheme.CYBERPUNK -> Color(0xFF141938)
        else -> Color(0xFF121925)
    },
        ThemeRuntime.cardBgTransparencyPercent
    )

val AvatarBackground: Color
    get() = applyTransparencyPercent(
        when {
        ThemeRuntime.currentTheme.isLightCategory() -> Color(0xFFE7ECF2)
        ThemeRuntime.currentTheme == AppTheme.CYBERPUNK && ThemeRuntime.neonLightBoostEnabled -> Color(0xFF21295A)
        ThemeRuntime.currentTheme == AppTheme.CYBERPUNK -> Color(0xFF1B2047)
        else -> Color(0xFF1C2634)
    },
        ThemeRuntime.cardBgTransparencyPercent
    )

private fun baseOnCardText(): Color {
    return when {
        ThemeRuntime.textColorOverride != null -> ThemeRuntime.textColorOverride!!.copy(alpha = 1f)
        ThemeRuntime.currentTheme.isLightCategory() && ThemeRuntime.highContrastTextEnabled -> Color(0xFF0D1520)
        ThemeRuntime.currentTheme.isLightCategory() -> Color(0xFF1B2430)
        ThemeRuntime.currentTheme == AppTheme.CYBERPUNK && ThemeRuntime.highContrastTextEnabled -> Color(0xFFFFFFFF)
        ThemeRuntime.currentTheme == AppTheme.CYBERPUNK -> Color(0xFFEAF8FF)
        ThemeRuntime.highContrastTextEnabled -> Color(0xFFFFFFFF)
        else -> Color(0xFFE8EAF0)
    }
}

val OnCardText: Color
    get() = applyTransparencyPercent(baseOnCardText(), ThemeRuntime.textTransparencyPercent)

val OnCardIcon: Color
    get() = baseOnCardText()

val ProgressTrack: Color
    get() = when {
        ThemeRuntime.currentTheme.isLightCategory() -> Color(0xFFD7DEE8)
        ThemeRuntime.currentTheme == AppTheme.CYBERPUNK && ThemeRuntime.neonLightBoostEnabled -> Color(0xFF344083)
        ThemeRuntime.currentTheme == AppTheme.CYBERPUNK -> Color(0xFF283064)
        else -> Color(0xFF2E3A4A)
    }

val SubtlePanel: Color
    get() = when {
        ThemeRuntime.currentTheme.isLightCategory() -> Color(0xFFECF2FA)
        ThemeRuntime.currentTheme == AppTheme.CYBERPUNK && ThemeRuntime.neonLightBoostEnabled -> Color(0xFF212A62)
        ThemeRuntime.currentTheme == AppTheme.CYBERPUNK -> Color(0xFF1A2050)
        else -> Color.Black.copy(alpha = 0.25f)
    }

fun readableTextColor(background: Color): Color {
    return if (background.luminance() > 0.48f) Color(0xFF111111) else Color.White
}

/* ===========================
   TYPOGRAPHY
   =========================== */

private fun applyFontFamily(t: Typography, family: FontFamily): Typography {
    return t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = family),
        displayMedium = t.displayMedium.copy(fontFamily = family),
        displaySmall = t.displaySmall.copy(fontFamily = family),
        headlineLarge = t.headlineLarge.copy(fontFamily = family),
        headlineMedium = t.headlineMedium.copy(fontFamily = family),
        headlineSmall = t.headlineSmall.copy(fontFamily = family),
        titleLarge = t.titleLarge.copy(fontFamily = family),
        titleMedium = t.titleMedium.copy(fontFamily = family),
        titleSmall = t.titleSmall.copy(fontFamily = family),
        bodyLarge = t.bodyLarge.copy(fontFamily = family),
        bodyMedium = t.bodyMedium.copy(fontFamily = family),
        bodySmall = t.bodySmall.copy(fontFamily = family),
        labelLarge = t.labelLarge.copy(fontFamily = family),
        labelMedium = t.labelMedium.copy(fontFamily = family),
        labelSmall = t.labelSmall.copy(fontFamily = family)
    )
}

fun createAppTypography(fontStyle: AppFontStyle): Typography {
    val family = when (fontStyle) {
        AppFontStyle.DEFAULT -> FontFamily.Default
        AppFontStyle.SANS -> FontFamily.SansSerif
        AppFontStyle.SERIF -> FontFamily.Serif
        AppFontStyle.MONO -> FontFamily.Monospace
        AppFontStyle.DISPLAY -> FontFamily.SansSerif
        AppFontStyle.ROUNDED -> FontFamily.SansSerif
        AppFontStyle.TERMINAL -> FontFamily.Monospace
        AppFontStyle.ELEGANT -> FontFamily.Serif
        AppFontStyle.HANDWRITTEN -> FontFamily.Cursive
    }
    val bodyWeight = when (fontStyle) {
        AppFontStyle.DISPLAY -> FontWeight.Medium
        AppFontStyle.TERMINAL -> FontWeight.Normal
        AppFontStyle.HANDWRITTEN -> FontWeight.Medium
        else -> FontWeight.Normal
    }
    val titleWeight = when (fontStyle) {
        AppFontStyle.DISPLAY -> FontWeight.ExtraBold
        AppFontStyle.ROUNDED -> FontWeight.SemiBold
        AppFontStyle.TERMINAL -> FontWeight.Medium
        AppFontStyle.ELEGANT -> FontWeight.SemiBold
        else -> FontWeight.Medium
    }
    val letterSpacing = when (fontStyle) {
        AppFontStyle.DISPLAY -> 0.35.sp
        AppFontStyle.TERMINAL -> 0.2.sp
        AppFontStyle.ELEGANT -> 0.25.sp
        AppFontStyle.HANDWRITTEN -> 0.1.sp
        else -> 0.sp
    }
    val bodySize = if (fontStyle == AppFontStyle.TERMINAL) 13.sp else 14.sp
    val base = Typography(
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = titleWeight,
            fontSize = 16.sp,
            letterSpacing = letterSpacing
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = bodyWeight,
            fontSize = bodySize,
            letterSpacing = letterSpacing
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = titleWeight,
            fontSize = 13.sp,
            letterSpacing = letterSpacing
        )
    )
    return applyFontFamily(base, family)
}

/* ===========================
   THEME
   =========================== */

@Composable
fun LivingLifeMMOTheme(
    theme: AppTheme = AppTheme.DEFAULT,
    accentOverride: Color? = null,
    backgroundOverride: Color? = null,
    cardColorOverride: Color? = null,
    highContrastTextEnabled: Boolean = false,
    reduceAnimationsEnabled: Boolean = false,
    compactModeEnabled: Boolean = false,
    largerTouchTargetsEnabled: Boolean = false,
    decorativeBordersEnabled: Boolean = false,
    neonLightBoostEnabled: Boolean = false,
    neonFlowEnabled: Boolean = true,
    neonFlowSpeed: Int = 0,
    neonGlowPalette: String = "magenta",
    fontStyle: AppFontStyle = AppFontStyle.DEFAULT,
    fontScalePercent: Int = 100,
    textColorOverride: Color? = null,
    content: @Composable () -> Unit
) {
    val baseDensity = LocalDensity.current
    ThemeRuntime.currentTheme = theme
    ThemeRuntime.highContrastTextEnabled = highContrastTextEnabled
    ThemeRuntime.reduceAnimationsEnabled = reduceAnimationsEnabled
    ThemeRuntime.compactModeEnabled = compactModeEnabled
    ThemeRuntime.largerTouchTargetsEnabled = largerTouchTargetsEnabled
    ThemeRuntime.decorativeBordersEnabled = decorativeBordersEnabled
    ThemeRuntime.neonLightBoostEnabled = neonLightBoostEnabled
    ThemeRuntime.neonFlowEnabled = neonFlowEnabled
    ThemeRuntime.neonFlowSpeed = neonFlowSpeed.coerceIn(0, 2)
    ThemeRuntime.neonGlowPalette = neonGlowPalette
    ThemeRuntime.fontStyle = fontStyle
    ThemeRuntime.fontScalePercent = fontScalePercent.coerceIn(80, 125)
    ThemeRuntime.textColorOverride = textColorOverride
    ThemeRuntime.cardColorOverride = cardColorOverride

    val baseColors = ThemeEngine.getColors(theme)
    val primary = accentOverride ?: baseColors.first
    val background = backgroundOverride ?: baseColors.second
    val cardColor = mixForBackground(primary, background).copy(alpha = if (theme.isLightCategory()) 0.08f else 0.15f)

    val lightOn = if (highContrastTextEnabled) Color(0xFF0D1520) else Color(0xFF1B2430)
    val cyberpunkTheme = theme == AppTheme.CYBERPUNK
    val scheme = if (theme.isLightCategory()) {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            secondary = Color(0xFF5F6B7A),
            onSecondary = Color.White,
            background = background,
            onBackground = lightOn,
            surface = Color(0xFFFFFFFF),
            onSurface = lightOn,
            surfaceVariant = Color(0xFFEAF0F7),
            onSurfaceVariant = if (highContrastTextEnabled) Color(0xFF243244) else Color(0xFF536074),
            outline = if (highContrastTextEnabled) Color(0xFF8C9CB0) else Color(0xFFB8C2D0),
            outlineVariant = Color(0xFFD6DDE8),
            error = Color(0xFFC62828),
            onError = Color.White
        )
    } else darkColorScheme(
        primary = primary,
        onPrimary = if (cyberpunkTheme) Color(0xFF050816) else background,
        secondary = if (cyberpunkTheme) {
            if (neonLightBoostEnabled) Color(0xFFFF5EB8) else Color(0xFFFF2E97)
        } else primary.copy(alpha = 0.85f),
        onSecondary = if (cyberpunkTheme) Color(0xFF0B0D1E) else background,
        tertiary = if (cyberpunkTheme) {
            if (neonLightBoostEnabled) Color(0xFF9B8BFF) else Color(0xFF7A5CFF)
        } else primary.copy(alpha = 0.70f),
        onTertiary = if (cyberpunkTheme) Color(0xFF0B0D1E) else background,
        background = background,
        onBackground = OnCardText,
        surface = CardDarkBlue,
        onSurface = OnCardText,
        surfaceVariant = if (cyberpunkTheme) mixForBackground(primary, CardDarkBlue).copy(alpha = 0.28f) else cardColor, // Use for subtle tinting
        onSurfaceVariant = OnCardText.copy(alpha = 0.80f),
        outline = if (cyberpunkTheme) primary.copy(alpha = if (neonLightBoostEnabled) 0.78f else 0.62f) else OnCardText.copy(alpha = 0.25f),
        outlineVariant = if (cyberpunkTheme) {
            (if (neonLightBoostEnabled) Color(0xFFFF72C5) else Color(0xFFFF2E97)).copy(alpha = if (neonLightBoostEnabled) 0.52f else 0.36f)
        } else OnCardText.copy(alpha = 0.12f),
        error = Color(0xFFFF6B6B),
        onError = background,
        inverseSurface = OnCardText,
        inverseOnSurface = background
    )

    val scaledDensity = Density(
        density = baseDensity.density,
        fontScale = (baseDensity.fontScale * (ThemeRuntime.fontScalePercent / 100f)).coerceIn(0.80f, 1.25f)
    )

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = scheme,
            typography = createAppTypography(fontStyle),
            content = content
        )
    }
}

/* ===========================
   UI/THEME HELPERS
   =========================== */

fun mixForBackground(accent: Color, background: Color): Color {
    return lerp(background, accent, 0.22f)
}

fun Color.toArgbCompat(): Int {
    val a = (alpha * 255f + 0.5f).toInt().coerceIn(0, 255)
    val r = (red * 255f + 0.5f).toInt().coerceIn(0, 255)
    val g = (green * 255f + 0.5f).toInt().coerceIn(0, 255)
    val b = (blue * 255f + 0.5f).toInt().coerceIn(0, 255)
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
