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

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    secondary = BlueAccent,
    tertiary = Purple80,
    background = PureBlack,
    surface = DarkCard,
    onBackground = WhiteText,
    onSurface = WhiteText,
    outline = DarkBorder
)

private val LightColorScheme = darkColorScheme( // Force dark theme styling as specified by the design direction
    primary = GoldAccent,
    secondary = BlueAccent,
    tertiary = Purple80,
    background = PureBlack,
    surface = DarkCard,
    onBackground = WhiteText,
    onSurface = WhiteText,
    outline = DarkBorder
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme to match the user's HTML design exactly
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve pure luxury dark aesthetic consistency
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
