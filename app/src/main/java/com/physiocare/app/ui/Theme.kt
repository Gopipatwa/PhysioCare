package com.physiocare.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Palette: calm clinic pine, warm sunflower for "you did it" moments, paper background.
val Pine = Color(0xFF0F5A5E)
val PineDark = Color(0xFF0A3F42)
val Mint = Color(0xFFD6ECE6)
val Sunflower = Color(0xFFF5B700)
val SunflowerSoft = Color(0xFFFFF1C2)
val Paper = Color(0xFFF3F6F2)
val Ink = Color(0xFF17201F)
val Coral = Color(0xFFC2412D)
val CoralSoft = Color(0xFFFBE3DE)

private val Colors = lightColorScheme(
    primary = Pine,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = PineDark,
    secondary = Sunflower,
    onSecondary = Color(0xFF2B2000),
    secondaryContainer = SunflowerSoft,
    onSecondaryContainer = Color(0xFF3D2E00),
    tertiary = Color(0xFF2F7D4F),
    onTertiary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE6ECE9),
    onSurfaceVariant = Color(0xFF3E4A48),
    outline = Color(0xFF6B7876),
    outlineVariant = Color(0xFFC5D0CD),
    error = Coral,
    onError = Color.White,
    errorContainer = CoralSoft,
    onErrorContainer = Color(0xFF5C1408),
)

// Larger than the Android defaults: body text starts at 20sp for easy reading.
private val Type = Typography(
    displayLarge = TextStyle(fontSize = 88.sp, lineHeight = 92.sp, fontWeight = FontWeight.Black),
    displayMedium = TextStyle(fontSize = 56.sp, lineHeight = 60.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 20.sp, lineHeight = 29.sp),
    bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 25.sp),
    bodySmall = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun PhysioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = Type, content = content)
}
