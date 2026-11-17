package com.example.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppLanguage
import com.example.domain.model.ThemeMode

private val MyJourneyLightColorScheme = lightColorScheme(
    primary = TurquoiseDeep,
    onPrimary = Color.White,
    primaryContainer = TurquoiseContainerLight,
    onPrimaryContainer = DeepNavy,
    secondary = AdventureOrange,
    onSecondary = Color.White,
    secondaryContainer = OrangeContainerLight,
    onSecondaryContainer = Color(0xFF4A1E05),
    tertiary = GoldDeep,
    onTertiary = Color.White,
    tertiaryContainer = GoldContainerLight,
    onTertiaryContainer = Color(0xFF3E2B00),
    background = LightCream,
    onBackground = TextDarkPrimary,
    surface = SoftCreamCard,
    onSurface = TextDarkPrimary,
    surfaceVariant = WarmCreamSurface,
    onSurfaceVariant = TextDarkSecondary,
    outline = Color(0xFFCBD4E1),
    outlineVariant = Color(0xFFE3E8F0),
    error = GentleCoral,
    onError = Color.White
)

private val MyJourneyDarkColorScheme = darkColorScheme(
    primary = TurquoisePrimary,
    onPrimary = DeepNavy,
    primaryContainer = TurquoiseContainerDark,
    onPrimaryContainer = Color(0xFFD5F7FF),
    secondary = AdventureOrangeSoft,
    onSecondary = Color(0xFF341400),
    secondaryContainer = OrangeContainerDark,
    onSecondaryContainer = Color(0xFFFFDEC9),
    tertiary = CompassGold,
    onTertiary = Color(0xFF2D1F00),
    tertiaryContainer = GoldContainerDark,
    onTertiaryContainer = Color(0xFFFFEBB8),
    background = DeepNavy,
    onBackground = TextLightPrimary,
    surface = MidnightNavy,
    onSurface = TextLightPrimary,
    surfaceVariant = SlateNavy,
    onSurfaceVariant = TextLightSecondary,
    outline = Color(0xFF334E7B),
    outlineVariant = Color(0xFF1F3863),
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF4B0C0C)
)

val MyJourneyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun MyJourneyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    language: AppLanguage = AppLanguage.PERSIAN,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) MyJourneyDarkColorScheme else MyJourneyLightColorScheme
    val isPersian = language == AppLanguage.PERSIAN
    val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildAppTypography(isPersian = isPersian),
            shapes = MyJourneyShapes,
            content = content
        )
    }
}
