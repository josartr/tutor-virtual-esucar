package cl.esucar.tutorvirtual.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object InstitutionalColors {
    val Green = Color(0xFF218265)
    val GreenDark = Color(0xFF17654F)
    val GreenSoft = Color(0xFFE6F1ED)
    val Lime = Color(0xFF8BAF3A)
    val Navy = Color(0xFF1F4768)
    val Graphite = Color(0xFF505050)
    val Background = Color(0xFFF4F4F4)
    val Border = Color(0xFFD8DDDA)
}

val InstitutionalLightColors: ColorScheme = lightColorScheme(
    primary = InstitutionalColors.Green,
    onPrimary = Color.White,
    primaryContainer = InstitutionalColors.GreenSoft,
    onPrimaryContainer = InstitutionalColors.GreenDark,
    secondary = InstitutionalColors.Lime,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAF2D7),
    onSecondaryContainer = Color(0xFF3A4E13),
    tertiary = InstitutionalColors.Navy,
    onTertiary = Color.White,
    background = InstitutionalColors.Background,
    onBackground = InstitutionalColors.Graphite,
    surface = Color.White,
    onSurface = InstitutionalColors.Graphite,
    surfaceVariant = Color(0xFFEEF1EF),
    onSurfaceVariant = Color(0xFF53605B),
    outline = InstitutionalColors.Border
)
