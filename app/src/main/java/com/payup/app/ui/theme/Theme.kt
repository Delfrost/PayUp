package com.payup.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Apple-inspired Monochromatic Color Tokens
val LightBackground = Color(0xFFFAFAFA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF3F3F3)
val LightTextPrimary = Color(0xFF171717)
val LightTextSecondary = Color(0xFF737373)
val LightTextTertiary = Color(0xFFA3A3A3)
val LightBorder = Color(0xFFE5E5E5)
val LightBorderSubtle = Color(0xFFEFEFEF)
val LightCardElevated = Color(0xFFFFFFFF)

val DarkBackground = Color(0xFF0B0B0B)
val DarkSurface = Color(0xFF181818)
val DarkSurfaceVariant = Color(0xFF222222)
val DarkTextPrimary = Color(0xFFEDEDED)
val DarkTextSecondary = Color(0xFFA1A1A1)
val DarkTextTertiary = Color(0xFF737373)
val DarkBorder = Color(0xFF282828)
val DarkBorderSubtle = Color(0xFF202020)
val DarkCardElevated = Color(0xFF1C1C1E)

// Semantic accent tints for icons/pills (subtle, high contrast)
val SemanticSuccess = Color(0xFF171717) // In monochromatic, completed/positive is crisp dark/light
val SemanticSuccessTint = Color(0xFFF0F0F0)
val SemanticDanger = Color(0xFF171717)
val SemanticDangerTint = Color(0xFFF4F4F4)
val SemanticWarning = Color(0xFF525252)
val SemanticWarningTint = Color(0xFFEAEAEA)

private val LightColorScheme = lightColorScheme(
    primary = LightTextPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF262626),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF404040),
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightTextPrimary,
    tertiary = Color(0xFF525252),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightBorderSubtle
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkTextPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF333333),
    onPrimaryContainer = Color.White,
    secondary = DarkTextSecondary,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = Color(0xFF8E8E8E),
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorderSubtle
)

// Clean Apple-inspired Typography System
val PayUpTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp
    )
)

object PayUpThemeExtended {
    val borderColor: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) DarkBorder else LightBorder

    val cardColor: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) DarkSurface else LightSurface

    val subtleBorderColor: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) DarkBorderSubtle else LightBorderSubtle

    val secondaryTextColor: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) DarkTextSecondary else LightTextSecondary
}

@Composable
fun PayUpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PayUpTypography,
        content = content
    )
}
