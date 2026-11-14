package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.model.StoreThemeOption

@Immutable
data class StoreSemanticColors(
    val verifiedBadge: Color = VerifiedBlue,
    val blockedBanner: Color = BlockedRed,
    val onlineIndicator: Color = OnlineGreen,
    val outgoingBubble: Color = EmeraldPrimaryContainer,
    val incomingBubble: Color = EmeraldSurfaceVariant,
    val accentGlow: Color = EmeraldPrimary,
)

val LocalStoreColors = staticCompositionLocalOf { StoreSemanticColors() }

fun colorSchemeForOption(option: StoreThemeOption): Pair<ColorScheme, StoreSemanticColors> =
    when (option) {
        StoreThemeOption.EMERALD -> darkColorScheme(
            primary = EmeraldPrimary,
            onPrimary = EmeraldOnPrimary,
            primaryContainer = EmeraldPrimaryContainer,
            onPrimaryContainer = EmeraldOnPrimaryContainer,
            secondary = EmeraldSecondary,
            onSecondary = Color(0xFF00210B),
            background = EmeraldBackground,
            onBackground = Color(0xFFE9EDEF),
            surface = EmeraldSurface,
            onSurface = Color(0xFFE9EDEF),
            surfaceVariant = EmeraldSurfaceVariant,
            onSurfaceVariant = Color(0xFF8696A0),
            surfaceContainerHigh = Color(0xFF202C33),
            outline = Color(0xFF2A3942),
            error = BlockedRed,
            onError = Color.White,
        ) to StoreSemanticColors(
            outgoingBubble = Color(0xFF005C4B),
            incomingBubble = Color(0xFF202C33),
            accentGlow = EmeraldPrimary,
        )

        StoreThemeOption.CRIMSON -> darkColorScheme(
            primary = CrimsonPrimary,
            onPrimary = CrimsonOnPrimary,
            primaryContainer = CrimsonPrimaryContainer,
            onPrimaryContainer = CrimsonOnPrimaryContainer,
            secondary = CrimsonSecondary,
            onSecondary = Color(0xFF2B0E00),
            background = CrimsonBackground,
            onBackground = Color(0xFFF8ECEE),
            surface = CrimsonSurface,
            onSurface = Color(0xFFF8ECEE),
            surfaceVariant = CrimsonSurfaceVariant,
            onSurfaceVariant = Color(0xFFBFA6AC),
            surfaceContainerHigh = Color(0xFF2F1D22),
            outline = Color(0xFF452930),
            error = Color(0xFFFF6B6B),
            onError = Color.Black,
        ) to StoreSemanticColors(
            outgoingBubble = Color(0xFF6B1822),
            incomingBubble = Color(0xFF2A1A1F),
            accentGlow = CrimsonPrimary,
        )

        StoreThemeOption.TITANIUM -> darkColorScheme(
            primary = TitaniumPrimary,
            onPrimary = TitaniumOnPrimary,
            primaryContainer = TitaniumPrimaryContainer,
            onPrimaryContainer = TitaniumOnPrimaryContainer,
            secondary = TitaniumSecondary,
            onSecondary = Color(0xFF00262B),
            background = TitaniumBackground,
            onBackground = Color(0xFFE6F0FA),
            surface = TitaniumSurface,
            onSurface = Color(0xFFE6F0FA),
            surfaceVariant = TitaniumSurfaceVariant,
            onSurfaceVariant = Color(0xFF93ADC6),
            surfaceContainerHigh = Color(0xFF1F2E40),
            outline = Color(0xFF2B3F56),
            error = BlockedRed,
            onError = Color.White,
        ) to StoreSemanticColors(
            outgoingBubble = Color(0xFF0C4A6E),
            incomingBubble = Color(0xFF1B2838),
            accentGlow = TitaniumPrimary,
        )

        StoreThemeOption.AMBER -> darkColorScheme(
            primary = AmberPrimary,
            onPrimary = AmberOnPrimary,
            primaryContainer = AmberPrimaryContainer,
            onPrimaryContainer = AmberOnPrimaryContainer,
            secondary = AmberSecondary,
            onSecondary = Color(0xFF2C1400),
            background = AmberBackground,
            onBackground = Color(0xFFF7F2E7),
            surface = AmberSurface,
            onSurface = Color(0xFFF7F2E7),
            surfaceVariant = AmberSurfaceVariant,
            onSurfaceVariant = Color(0xFFB7AA92),
            surfaceContainerHigh = Color(0xFF332B1D),
            outline = Color(0xFF473C29),
            error = BlockedRed,
            onError = Color.White,
        ) to StoreSemanticColors(
            outgoingBubble = Color(0xFF663B08),
            incomingBubble = Color(0xFF2B251A),
            accentGlow = AmberPrimary,
        )

        StoreThemeOption.VIOLET -> darkColorScheme(
            primary = VioletPrimary,
            onPrimary = VioletOnPrimary,
            primaryContainer = VioletPrimaryContainer,
            onPrimaryContainer = VioletOnPrimaryContainer,
            secondary = VioletSecondary,
            onSecondary = Color(0xFF33061E),
            background = VioletBackground,
            onBackground = Color(0xFFF0ECFA),
            surface = VioletSurface,
            onSurface = Color(0xFFF0ECFA),
            surfaceVariant = VioletSurfaceVariant,
            onSurfaceVariant = Color(0xFFA99BC2),
            surfaceContainerHigh = Color(0xFF2B2140),
            outline = Color(0xFF3D2F59),
            error = BlockedRed,
            onError = Color.White,
        ) to StoreSemanticColors(
            outgoingBubble = Color(0xFF431E78),
            incomingBubble = Color(0xFF241C35),
            accentGlow = VioletPrimary,
        )
    }

@Composable
fun TurboExhaustTheme(
    themeOption: StoreThemeOption = StoreThemeOption.EMERALD,
    content: @Composable () -> Unit,
) {
    val (colorScheme, semanticColors) = colorSchemeForOption(themeOption)
    CompositionLocalProvider(LocalStoreColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    TurboExhaustTheme(content = content)
}
