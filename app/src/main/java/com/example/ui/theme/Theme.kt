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

import androidx.compose.ui.graphics.Color

private val LightColorScheme =
  lightColorScheme(
    primary = BluePrimary,
    onPrimary = OnBluePrimary,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = OnBluePrimaryContainer,
    secondary = GreenSecondary,
    onSecondary = OnGreenSecondary,
    secondaryContainer = GreenSecondaryContainer,
    onSecondaryContainer = OnGreenSecondaryContainer,
    background = SlateBackground,
    onBackground = TextDark,
    surface = CardWhite,
    onSurface = TextDark,
    error = WarningRed,
    errorContainer = WarningRedLight,
    onError = Color.White
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = LightColorScheme, typography = Typography, content = content)
}
