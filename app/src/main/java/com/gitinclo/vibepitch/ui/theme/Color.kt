package com.gitinclo.vibepitch.ui.theme

import androidx.compose.ui.graphics.Color

val ObsidianSlate = Color(0xFF0F172A)
val DeepSlateSurface = Color(0xFF1E293B)
val ElevatedSlateCard = Color(0xFF26334D)
val ElectricIndigo = Color(0xFF4F46E5)
val ElectricIndigoLight = Color(0xFF818CF8)
val EmeraldConversion = Color(0xFF10B981)
val EmeraldMintLight = Color(0xFF34D399)
val AmberHighlight = Color(0xFFF59E0B)
val WarmIvoryBackground = Color(0xFFF8FAFC)
val PureWhiteSurface = Color(0xFFFFFFFF)
val SoftSlateVariant = Color(0xFFE2E8F0)
val SlateInkText = Color(0xFF0F172A)
val MutedSlateText = Color(0xFF475569)
val LightMutedText = Color(0xFF94A3B8)
val CoralError = Color(0xFFEF4444)

val BrandSwatchOrange = Color(0xFFF97316)
val BrandSwatchBlue = Color(0xFF3B82F6)
val BrandSwatchEmerald = Color(0xFF10B981)
val BrandSwatchIndigo = Color(0xFF6366F1)
val BrandSwatchRose = Color(0xFFF43F5E)
val BrandSwatchAmber = Color(0xFFD97706)

fun parseBrandColor(hex: String, fallback: Color = ElectricIndigo): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        val longVal = clean.toLong(16)
        when (clean.length) {
            6 -> Color(longVal or 0xFF000000L)
            8 -> Color(longVal)
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
