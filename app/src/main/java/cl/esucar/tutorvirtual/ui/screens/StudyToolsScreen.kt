@file:OptIn(ExperimentalLayoutApi::class)

package cl.esucar.tutorvirtual.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.esucar.tutorvirtual.domain.model.RadioPlaybackState
import cl.esucar.tutorvirtual.ui.state.ApaGeneratorUiState
import cl.esucar.tutorvirtual.ui.state.PomodoroUiState
import cl.esucar.tutorvirtual.ui.theme.InstitutionalColors

@Composable
fun StudyToolsScreen(
    pomodoro: PomodoroUiState,
    apaGenerator: ApaGeneratorUiState,
    radioState: RadioPlaybackState,
    radioError: String?,
    onStartPomodoro: () -> Unit,
    onPausePomodoro: () -> Unit,
    onResetPomodoro: () -> Unit,
    onApaTypeSelected: (String) -> Unit,
    onApaAuthorChanged: (String) -> Unit,
    onApaYearChanged: (String) -> Unit,
    onApaTitleChanged: (String) -> Unit,
    onApaSourceChanged: (String) -> Unit,
    onCopyCitation: () -> Unit,
    onOpenMuseo: () -> Unit,
    onToggleRadio: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Text("Herramientas de Estudio", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Métodos y utilidades pedagógicas para optimizar tu tiempo académico.")
        }

        // --- SECTION A: POMODORO TIMER ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = InstitutionalColors.GreenSoft),
                border = BorderStroke(1.dp, InstitutionalColors.Green.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = InstitutionalColors.White.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            pomodoro.mode.label,
                            color = InstitutionalColors.GreenDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))

                    Text(
                        pomodoro.formattedTime,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        color = InstitutionalColors.GreenDark
                    )

                    Text(
                        pomodoro.mode.helperText,
                        fontSize = 13.sp,
                        color = InstitutionalColors.MutedText
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!pomodoro.isRunning) {
                            Button(
                                onClick = onStartPomodoro,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = InstitutionalColors.Green,
                                    contentColor = InstitutionalColors.White
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Iniciar", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Button(
                                onClick = onPausePomodoro,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF1D7AF),
                                    contentColor = Color(0xFF533914)
                                )
                            ) {
                                Icon(Icons.Default.Pause, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Pausar", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        OutlinedButton(
                            onClick = onResetPomodoro,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, InstitutionalColors.Green.copy(alpha = 0.35f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = InstitutionalColors.GreenDark)
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Reiniciar")
                        }
                    }
                }
            }
        }

        // --- SECTION B: GENERADOR CITAS APA 7 INSTITUCIONAL ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                border = BorderStroke(1.dp, InstitutionalColors.Border),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Generador de Citas APA 7 (Leyes y Reglamentos)", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        apaGenerator.availableTypes.forEach { type ->
                            FilterChip(
                                selected = apaGenerator.selectedType == type,
                                onClick = { onApaTypeSelected(type) },
                                label = { Text(type) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = apaGenerator.author,
                        onValueChange = onApaAuthorChanged,
                        singleLine = true,
                        label = {
                            Text(
                                if (apaGenerator.selectedType == "Ley") {
                                    "Ministerio / Emisor (Ej. Min. Defensa)"
                                } else {
                                    "Autor / Institución"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = apaGenerator.year,
                            onValueChange = onApaYearChanged,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text("Año") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = apaGenerator.title,
                            onValueChange = onApaTitleChanged,
                            singleLine = true,
                            label = { Text(if (apaGenerator.selectedType == "Ley") "N° Ley / Título" else "Título") },
                            modifier = Modifier.weight(2f)
                        )
                    }

                    OutlinedTextField(
                        value = apaGenerator.source,
                        onValueChange = onApaSourceChanged,
                        singleLine = true,
                        label = { Text("Fuente / Repositorio (Ej. Biblioteca del Congreso)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(4.dp))

                    Surface(
                        color = InstitutionalColors.GreenSoft,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "Cita Generada en Formato APA 7:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = InstitutionalColors.GreenDark
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(apaGenerator.generatedCitation, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = onCopyCitation,
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = InstitutionalColors.Green)
                            ) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copiar Cita", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION C: MATRIZ DE CONSISTENCIA ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                border = BorderStroke(1.dp, InstitutionalColors.Border),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Guía de Matriz de Consistencia Metodológica", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Asegúrate de que los 5 elementos de tu Trabajo de Título estén alineados:",
                        fontSize = 13.sp,
                        color = InstitutionalColors.Graphite
                    )

                    listOf(
                        "1. Problema" to "Situación operacional/policial que requiere diagnóstico o propuesta de solución.",
                        "2. Pregunta" to "¿Cómo/Qué/De qué manera influye el problema en la función institucional?",
                        "3. Obj. General" to "Verbo en infinitivo + responde directamente a la pregunta de investigación.",
                        "4. Obj. Específicos" to "Pasos ordenados para cumplir el objetivo general (ej. Diagnosticar, Analizar, Proponer).",
                        "5. Metodología" to "Enfoque (cuantitativo/cualitativo), población/muestra e instrumentos de recolección."
                    ).forEach { (title, desc) ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text(
                                title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = InstitutionalColors.GreenDark,
                                modifier = Modifier.width(108.dp)
                            )
                            Text(desc, fontSize = 13.sp, color = InstitutionalColors.Graphite)
                        }
                    }
                }
            }
        }

        // --- SECTION D: RECURSOS CULTURALES Y RADIO ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                border = BorderStroke(1.dp, InstitutionalColors.Border),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Recursos Culturales y Audiovisuales",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = InstitutionalColors.GreenDark
                        )
                        Text(
                            "Patrimonio institucional y señal radial oficial en vivo de Carabineros de Chile.",
                            fontSize = 12.sp,
                            color = InstitutionalColors.MutedText
                        )
                    }

                    Surface(
                        color = InstitutionalColors.GreenSoft,
                        border = BorderStroke(1.dp, InstitutionalColors.Green.copy(alpha = 0.16f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenMuseo() }
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = InstitutionalColors.Green,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Museum, contentDescription = null, tint = InstitutionalColors.White)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Museo Histórico de Carabineros",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = InstitutionalColors.GreenDark
                                )
                                Text(
                                    "Explora la historia y patrimonio institucional en línea",
                                    fontSize = 12.sp,
                                    color = InstitutionalColors.MutedText
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Abrir sitio",
                                tint = InstitutionalColors.GreenDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    val isRadioPlaying = radioState == RadioPlaybackState.PLAYING
                    val isRadioPaused = radioState == RadioPlaybackState.PAUSED
                    val isRadioBuffering = radioState == RadioPlaybackState.BUFFERING
                    val isRadioError = radioState == RadioPlaybackState.ERROR

                    Surface(
                        color = if (isRadioPlaying) InstitutionalColors.GreenSoft else InstitutionalColors.NavySoft,
                        border = BorderStroke(
                            if (isRadioPlaying) 1.5.dp else 1.dp,
                            if (isRadioPlaying) InstitutionalColors.Green.copy(alpha = 0.45f)
                            else InstitutionalColors.Navy.copy(alpha = 0.18f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onToggleRadio() }
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (isRadioPlaying) InstitutionalColors.Green else InstitutionalColors.Navy,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Radio, contentDescription = null, tint = InstitutionalColors.White)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Radio Carabineros de Chile",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (isRadioPlaying) InstitutionalColors.GreenDark else InstitutionalColors.Navy
                                )
                                Text(
                                    when {
                                        isRadioPlaying -> "Reproduciendo señal en vivo en la app"
                                        isRadioPaused -> "En pausa · Toca para reanudar"
                                        isRadioBuffering -> "Conectando transmisión en vivo..."
                                        isRadioError -> radioError ?: "No se pudo conectar. Toca para reintentar."
                                        else -> "Toca para escuchar en vivo aquí mismo"
                                    },
                                    fontSize = 12.sp,
                                    color = if (isRadioError) MaterialTheme.colorScheme.error else InstitutionalColors.MutedText
                                )
                                Spacer(Modifier.height(6.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(color = InstitutionalColors.White, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            "FM 98.1",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = InstitutionalColors.Navy,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(color = InstitutionalColors.White, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            "AM 820",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = InstitutionalColors.GreenDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = if (isRadioPlaying) InstitutionalColors.Green else InstitutionalColors.Navy,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isRadioBuffering) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = InstitutionalColors.White
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            "Conectando",
                                            color = InstitutionalColors.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else {
                                        Icon(
                                            when {
                                                isRadioPlaying -> Icons.Default.Pause
                                                isRadioError -> Icons.Default.Refresh
                                                else -> Icons.Default.PlayArrow
                                            },
                                            contentDescription = if (isRadioPlaying) "Pausar radio" else "Escuchar radio en vivo",
                                            tint = InstitutionalColors.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            when {
                                                isRadioPlaying -> "Pausar"
                                                isRadioPaused -> "Reanudar"
                                                isRadioError -> "Reintentar"
                                                else -> "Escuchar"
                                            },
                                            color = InstitutionalColors.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
