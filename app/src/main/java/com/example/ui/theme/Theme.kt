package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = ElectricIndigoLight,
    onPrimary = ObsidianSlate,
    primaryContainer = ElectricIndigo,
    onPrimaryContainer = Color.White,
    secondary = EmeraldMintLight,
    onSecondary = ObsidianSlate,
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = AmberHighlight,
    onTertiary = ObsidianSlate,
    background = ObsidianSlate,
    onBackground = Color(0xFFF8FAFC),
    surface = DeepSlateSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = ElevatedSlateCard,
    onSurfaceVariant = LightMutedText,
    error = CoralError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = EmeraldConversion,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = AmberHighlight,
    onTertiary = Color.White,
    background = WarmIvoryBackground,
    onBackground = SlateInkText,
    surface = PureWhiteSurface,
    onSurface = SlateInkText,
    surfaceVariant = SoftSlateVariant,
    onSurfaceVariant = MutedSlateText,
    error = CoralError,
    onError = Color.White
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
