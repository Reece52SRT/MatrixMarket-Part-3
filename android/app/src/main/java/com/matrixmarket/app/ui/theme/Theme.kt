package com.matrixmarket.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// One consistent colour scheme + type scale used by every screen.
private val MatrixMarketColorScheme = lightColorScheme(
    primary = RoyalPurple,
    onPrimary = White,
    primaryContainer = LavenderCard,
    onPrimaryContainer = TextDark,
    secondary = NeonPurple,
    onSecondary = White,
    tertiary = MeetingTeal,
    background = LavenderLight,
    onBackground = TextDark,
    surface = White,
    onSurface = TextDark,
    surfaceVariant = LavenderCard,
    onSurfaceVariant = TextMuted,
    error = ErrorRed,
    onError = White
)

private val MatrixMarketTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
)

@Composable
fun MatrixMarketTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MatrixMarketColorScheme,
        typography = MatrixMarketTypography,
        content = content
    )
}
