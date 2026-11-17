package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class EmberPalette(
    val isOledBlack: Boolean,
    val canvasBackground: Color,
    val recessedSurface: Color,
    val glassSurface: Color,
    val glassBorderOuter: Color,
    val glassBorderInner: Color,
    val gridDot: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkMuted: Color,
    val signalCrimson: Color,
    val signalCrimsonTint: Color,
    val coralThermal: Color,
    val iceCobalt: Color,
    val iceCobaltTint: Color,
    val skyFrost: Color,
    val safeEmerald: Color,
    val safeEmeraldTint: Color,
    val urgentRed: Color,
    val urgentRedTint: Color,
)

private val PearlMistPalette = EmberPalette(
    isOledBlack = false,
    canvasBackground = PearlMistBackground,
    recessedSurface = CoolRecessSurface,
    glassSurface = FrostedGlassWhite,
    glassBorderOuter = GlassBorderOuter,
    glassBorderInner = GlassBorderHighlight,
    gridDot = GridDotLight,
    inkPrimary = ObsidianInk,
    inkSecondary = GraphiteSlate,
    inkMuted = MutedSteel,
    signalCrimson = SignalCrimson,
    signalCrimsonTint = SignalCrimsonTint,
    coralThermal = CoralThermal,
    iceCobalt = IceCobalt,
    iceCobaltTint = IceCobaltTint,
    skyFrost = SkyFrost,
    safeEmerald = SafeEmerald,
    safeEmeraldTint = SafeEmeraldTint,
    urgentRed = UrgentRed,
    urgentRedTint = UrgentRedTint,
)

private val OledBlackPalette = EmberPalette(
    isOledBlack = true,
    canvasBackground = OledTrueBlack,
    recessedSurface = OledRecessSurface,
    glassSurface = OledGlassSurface,
    glassBorderOuter = OledBorderOuter,
    glassBorderInner = OledBorderHighlight,
    gridDot = OledGridDot,
    inkPrimary = OledWhiteInk,
    inkSecondary = OledSlateInk,
    inkMuted = OledMutedInk,
    signalCrimson = SignalCrimson,
    signalCrimsonTint = SignalCrimsonTintDark,
    coralThermal = CoralThermal,
    iceCobalt = IceCobaltBright,
    iceCobaltTint = IceCobaltTintDark,
    skyFrost = SkyFrost,
    safeEmerald = SafeEmeraldBright,
    safeEmeraldTint = SafeEmeraldTintDark,
    urgentRed = UrgentRedBright,
    urgentRedTint = UrgentRedTintDark,
)

val LocalEmberPalette = staticCompositionLocalOf { PearlMistPalette }

object EmberTheme {
    val palette: EmberPalette
        @Composable get() = LocalEmberPalette.current
}

private val EmberLightColorScheme = lightColorScheme(
    primary = SignalCrimson,
    onPrimary = Color.White,
    primaryContainer = SignalCrimsonTint,
    onPrimaryContainer = SignalCrimsonDark,
    secondary = IceCobalt,
    onSecondary = Color.White,
    secondaryContainer = IceCobaltTint,
    onSecondaryContainer = IceCobalt,
    tertiary = CoralThermal,
    onTertiary = Color.White,
    background = PearlMistBackground,
    onBackground = ObsidianInk,
    surface = FrostedGlassWhite,
    onSurface = ObsidianInk,
    surfaceVariant = CoolRecessSurface,
    onSurfaceVariant = GraphiteSlate,
    outline = GlassBorderOuter,
    outlineVariant = MutedSteel,
    error = UrgentRed,
    onError = Color.White,
    errorContainer = UrgentRedTint,
    onErrorContainer = UrgentRed,
)

private val EmberOledColorScheme = darkColorScheme(
    primary = SignalCrimson,
    onPrimary = Color.White,
    primaryContainer = SignalCrimsonTintDark,
    onPrimaryContainer = Color.White,
    secondary = IceCobaltBright,
    onSecondary = Color.White,
    secondaryContainer = IceCobaltTintDark,
    onSecondaryContainer = OledWhiteInk,
    tertiary = CoralThermal,
    onTertiary = Color.White,
    background = OledTrueBlack,
    onBackground = OledWhiteInk,
    surface = OledGlassSurface,
    onSurface = OledWhiteInk,
    surfaceVariant = OledRecessSurface,
    onSurfaceVariant = OledSlateInk,
    outline = OledBorderOuter,
    outlineVariant = OledMutedInk,
    error = UrgentRedBright,
    onError = Color.White,
    errorContainer = UrgentRedTintDark,
    onErrorContainer = OledWhiteInk,
)

@Composable
fun EmberCoreTheme(
    useOledBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = if (useOledBlack) OledBlackPalette else PearlMistPalette
    val colorScheme = if (useOledBlack) EmberOledColorScheme else EmberLightColorScheme

    CompositionLocalProvider(LocalEmberPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
