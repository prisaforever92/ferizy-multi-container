package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = CyanPrimaryDark,
    onPrimary = OnCyanPrimaryDark,
    primaryContainer = CyanContainerDark,
    onPrimaryContainer = OnCyanContainerDark,
    secondary = CobaltSecondaryDark,
    onSecondary = OnCobaltDark,
    secondaryContainer = CobaltContainerDark,
    onSecondaryContainer = OnCobaltContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = OnAmberDark,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = OnAmberContainerDark,
    background = StudioBgDark,
    onBackground = StudioTextPrimaryDark,
    surface = StudioSurfaceDark,
    onSurface = StudioTextPrimaryDark,
    surfaceVariant = StudioSurfaceVariantDark,
    onSurfaceVariant = StudioTextSecondaryDark,
    outline = StudioOutlineDark,
    outlineVariant = StudioOutlineDark.copy(alpha = 0.6f),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NavyPrimaryLight,
    onPrimary = OnNavyLight,
    primaryContainer = NavyContainerLight,
    onPrimaryContainer = OnNavyContainerLight,
    secondary = CyanSecondaryLight,
    onSecondary = OnCyanLight,
    secondaryContainer = CyanContainerLight,
    onSecondaryContainer = OnCyanContainerLight,
    tertiary = AmberTertiaryLight,
    onTertiary = OnAmberLight,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = OnAmberContainerLight,
    background = StudioBgLight,
    onBackground = StudioTextPrimaryLight,
    surface = StudioSurfaceLight,
    onSurface = StudioTextPrimaryLight,
    surfaceVariant = StudioSurfaceVariantLight,
    onSurfaceVariant = StudioTextSecondaryLight,
    outline = StudioOutlineLight,
    outlineVariant = StudioOutlineLight.copy(alpha = 0.6f),
  )

val FerizyForgeShapes =
  Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun FerizyForgeTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    shapes = FerizyForgeShapes,
    content = content,
  )
}
