package com.airgorah.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CyberBg = Color(0xFF0B0F14)
val CyberSurface = Color(0xFF131A22)
val CyberSurfaceVariant = Color(0xFF1B2430)
val CyberCyan = Color(0xFF38BDF8)
val CyberEmerald = Color(0xFF10B981)
val CyberAmber = Color(0xFFF59E0B)
val CyberRed = Color(0xFFEF4444)
val CyberPurple = Color(0xFFA855F7)
val CyberText = Color(0xFFE2E8F0)
val CyberTextMuted = Color(0xFF94A3B8)

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF001E2B),
    secondary = CyberEmerald,
    onSecondary = Color(0xFF002114),
    tertiary = CyberAmber,
    background = CyberBg,
    onBackground = CyberText,
    surface = CyberSurface,
    onSurface = CyberText,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = CyberTextMuted,
    error = CyberRed,
    onError = Color.White,
    outline = Color(0xFF2D3A4B)
)

private val AirgorahTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 0.2.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

@Composable
fun AirgorahTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AirgorahTypography,
        content = content
    )
}
