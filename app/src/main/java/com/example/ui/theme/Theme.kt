package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = JarvisCyan,
    onPrimary = JarvisBase,
    secondary = JarvisBlue,
    onSecondary = JarvisBase,
    tertiary = JarvisGold,
    background = JarvisBackground,
    surface = JarvisSlate,
    onBackground = JarvisTextPrimary,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisSlateLight,
    onSurfaceVariant = JarvisTextSecondary
  )

private val LightColorScheme = DarkColorScheme // Jarvis is exclusively an immersive dark experience

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for the Jarvis HUD experience
  dynamicColor: Boolean = false, // Disable dynamic system colors to prevent high-tech UI dilution
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
