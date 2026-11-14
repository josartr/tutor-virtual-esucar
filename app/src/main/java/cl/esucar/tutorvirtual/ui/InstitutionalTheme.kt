package cl.esucar.tutorvirtual.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object InstitutionalColors {
    val Green = Color(0xFF1B5E3F)
    val GreenDark = Color(0xFF12402B)
    val GreenSoft = Color(0xFFEAF2ED)
    val Lime = Color(0xFF7CA153)
    val Navy = Color(0xFF284B63)
    val NavySoft = Color(0xFFEBF1F5)
    val Graphite = Color(0xFF3A443E)
    val MutedText = Color(0xFF64716A)
    val Background = Color(0xFFF6F8F6)
    val SurfaceTint = Color(0xFFF0F4F1)
    val Border = Color(0xFFE1E7E3)
    val White = Color.White
}

val InstitutionalLightColors: ColorScheme = lightColorScheme(
    primary = InstitutionalColors.Green,
    onPrimary = Color.White,
    primaryContainer = InstitutionalColors.GreenSoft,
    onPrimaryContainer = InstitutionalColors.GreenDark,
    secondary = InstitutionalColors.Lime,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECF3E4),
    onSecondaryContainer = Color(0xFF354A20),
    tertiary = InstitutionalColors.Navy,
    onTertiary = Color.White,
    background = InstitutionalColors.Background,
    onBackground = InstitutionalColors.Graphite,
    surface = Color.White,
    onSurface = InstitutionalColors.Graphite,
    surfaceVariant = InstitutionalColors.SurfaceTint,
    onSurfaceVariant = InstitutionalColors.MutedText,
    outline = InstitutionalColors.Border,
    error = Color(0xFFB33A32),
    onError = Color.White
)
