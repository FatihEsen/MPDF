package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CatppuccinMochaColorScheme = darkColorScheme(
  primary = MochaPeach,
  onPrimary = MochaCrust,
  primaryContainer = MochaSurface0,
  onPrimaryContainer = MochaPeach,
  secondary = MochaMauve,
  onSecondary = MochaCrust,
  secondaryContainer = MochaSurface1,
  onSecondaryContainer = MochaMauve,
  tertiary = MochaGreen,
  onTertiary = MochaCrust,
  tertiaryContainer = MochaSurface0,
  onTertiaryContainer = MochaGreen,
  background = MochaBase,
  onBackground = MochaText,
  surface = MochaMantle,
  onSurface = MochaText,
  surfaceVariant = MochaSurface0,
  onSurfaceVariant = MochaSubtext0,
  outline = MochaSurface1,
  outlineVariant = MochaSurface0,
  error = MochaRed,
  onError = MochaCrust,
  errorContainer = MochaSurface0,
  onErrorContainer = MochaRed
)

@Composable
fun MpdfTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = CatppuccinMochaColorScheme,
    typography = Typography,
    content = content
  )
}
