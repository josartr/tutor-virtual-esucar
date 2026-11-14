package cl.esucar.tutorvirtual.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.esucar.tutorvirtual.domain.model.RadioPlaybackState
import cl.esucar.tutorvirtual.ui.theme.InstitutionalColors

@Composable
fun MiniRadioBar(
    radioState: RadioPlaybackState,
    radioError: String?,
    radioVolume: Float,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit
) {
    val visible = radioState != RadioPlaybackState.STOPPED

    AnimatedVisibility(visible = visible) {
        Surface(
            color = InstitutionalColors.NavySoft,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(thickness = 1.dp, color = InstitutionalColors.Border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (radioState == RadioPlaybackState.PLAYING) {
                            InstitutionalColors.Green
                        } else {
                            InstitutionalColors.Navy
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Radio,
                                contentDescription = null,
                                tint = InstitutionalColors.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Radio Carabineros de Chile",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InstitutionalColors.Navy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when (radioState) {
                                RadioPlaybackState.PLAYING -> "En vivo · FM 98.1 / AM 820"
                                RadioPlaybackState.PAUSED -> "En pausa · FM 98.1 / AM 820"
                                RadioPlaybackState.BUFFERING -> "Conectando transmisión en vivo..."
                                RadioPlaybackState.ERROR -> radioError ?: "Error de conexión"
                                RadioPlaybackState.STOPPED -> ""
                            },
                            fontSize = 11.sp,
                            color = if (radioState == RadioPlaybackState.ERROR) {
                                MaterialTheme.colorScheme.error
                            } else {
                                InstitutionalColors.MutedText
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    when (radioState) {
                        RadioPlaybackState.BUFFERING -> {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(20.dp),
                                strokeWidth = 2.dp,
                                color = InstitutionalColors.Navy
                            )
                        }
                        RadioPlaybackState.ERROR -> {
                            IconButton(
                                onClick = onPlay,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Reintentar radio",
                                    tint = InstitutionalColors.Navy,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        RadioPlaybackState.PLAYING -> {
                            FilledTonalIconButton(
                                onClick = onPause,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = InstitutionalColors.GreenSoft,
                                    contentColor = InstitutionalColors.GreenDark
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Pause,
                                    contentDescription = "Pausar radio",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        RadioPlaybackState.PAUSED -> {
                            FilledTonalIconButton(
                                onClick = onResume,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = InstitutionalColors.Green,
                                    contentColor = InstitutionalColors.White
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Reproducir radio",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        RadioPlaybackState.STOPPED -> Unit
                    }
                    Spacer(Modifier.width(2.dp))
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar reproductor",
                            tint = InstitutionalColors.MutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (radioState != RadioPlaybackState.ERROR) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 16.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    radioVolume <= 0.01f -> Icons.AutoMirrored.Filled.VolumeOff
                                    radioVolume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                                    else -> Icons.AutoMirrored.Filled.VolumeUp
                                },
                                contentDescription = if (radioVolume <= 0.01f) "Activar sonido" else "Silenciar",
                                tint = InstitutionalColors.Navy,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Slider(
                            value = radioVolume,
                            onValueChange = onVolumeChange,
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = InstitutionalColors.Green,
                                activeTrackColor = InstitutionalColors.Green,
                                inactiveTrackColor = InstitutionalColors.Border
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${(radioVolume * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = InstitutionalColors.MutedText,
                            modifier = Modifier.width(34.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
