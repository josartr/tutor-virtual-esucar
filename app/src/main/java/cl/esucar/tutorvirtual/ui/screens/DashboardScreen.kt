package cl.esucar.tutorvirtual.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.esucar.tutorvirtual.domain.model.DeliverableStatus
import cl.esucar.tutorvirtual.domain.model.ProjectTopic
import cl.esucar.tutorvirtual.domain.model.RadioPlaybackState
import cl.esucar.tutorvirtual.ui.components.DeliverableCardItem
import cl.esucar.tutorvirtual.ui.components.QuickAction
import cl.esucar.tutorvirtual.ui.components.SectionTitle
import cl.esucar.tutorvirtual.ui.state.MainUiState
import cl.esucar.tutorvirtual.ui.theme.InstitutionalColors

@Composable
fun DashboardScreen(
    state: MainUiState,
    onRadioClick: () -> Unit,
    onRevistaClick: () -> Unit,
    onMuseoClick: () -> Unit,
    onAddTopicClick: () -> Unit,
    onDeleteTopicClick: (ProjectTopic) -> Unit,
    onOpenProfileClick: () -> Unit,
    onNavigateToGuide: () -> Unit,
    onDeliverableStatusChange: (String, DeliverableStatus) -> Unit
) {
    val profile = state.userProfile
    val summary = state.progressSummary
    val progress by animateFloatAsState(summary.fraction, label = "progress")

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnimatedVisibility(true, enter = fadeIn() + slideInVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Hola, ${profile.rank} ${profile.fullName}".trim(),
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = InstitutionalColors.GreenDark
                    )
                    Text(
                        "${profile.userRole.label} · ${profile.institutionType.label} · Avanza con claridad en tu Trabajo de Título.",
                        color = InstitutionalColors.MutedText,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = InstitutionalColors.GreenSoft),
                border = BorderStroke(1.dp, InstitutionalColors.Green.copy(alpha = 0.18f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Progreso general del proyecto",
                        fontWeight = FontWeight.SemiBold,
                        color = InstitutionalColors.GreenDark,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${(progress * 100).toInt()}% de avance general",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = InstitutionalColors.GreenDark
                    )
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = InstitutionalColors.Green,
                        trackColor = InstitutionalColors.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${summary.approvedDeliverables} de ${state.deliverables.size} hitos aprobados · " +
                            "${summary.completedChecklistItems} de ${summary.totalChecklistItems} tareas completadas",
                        color = InstitutionalColors.MutedText,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            SectionTitle("Accesos rápidos", null) {}
        }

        item {
            val isRadioActive = state.radioState == RadioPlaybackState.PLAYING ||
                state.radioState == RadioPlaybackState.BUFFERING
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(
                    label = when (state.radioState) {
                        RadioPlaybackState.PLAYING -> "Radio\nEn vivo"
                        RadioPlaybackState.PAUSED -> "Radio\nEn pausa"
                        RadioPlaybackState.BUFFERING -> "Radio\nConectando"
                        else -> "Radio\nCarabineros"
                    },
                    icon = Icons.Default.Radio,
                    highlighted = isRadioActive,
                    onClick = onRadioClick
                )

                QuickAction(
                    label = "Revista\nCarabineros",
                    icon = Icons.Default.Newspaper,
                    onClick = onRevistaClick
                )

                QuickAction(
                    label = "Museo de\nCarabineros",
                    icon = Icons.Default.Museum,
                    onClick = onMuseoClick
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Registro de temas de proyecto",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = InstitutionalColors.Graphite
                    )
                    Text(
                        "No modificables · Solo puedes añadir o eliminar registros",
                        fontSize = 12.sp,
                        color = InstitutionalColors.MutedText
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onAddTopicClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = InstitutionalColors.GreenSoft,
                        contentColor = InstitutionalColors.GreenDark
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (state.projectTopics.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                    border = BorderStroke(1.dp, InstitutionalColors.Border),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = InstitutionalColors.MutedText,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            "Sin temas registrados",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = InstitutionalColors.Graphite
                        )
                        Text(
                            "Toca 'Añadir' para inscribir un tema oficial de proyecto.",
                            fontSize = 12.sp,
                            color = InstitutionalColors.MutedText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(state.projectTopics, key = { it.id }) { registeredTopic ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                    border = BorderStroke(1.dp, InstitutionalColors.Border),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = InstitutionalColors.SurfaceTint,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = InstitutionalColors.GreenDark,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "No editable",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = InstitutionalColors.GreenDark
                                        )
                                    }
                                }
                                Text(
                                    registeredTopic.createdAt,
                                    fontSize = 11.sp,
                                    color = InstitutionalColors.MutedText
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = registeredTopic.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InstitutionalColors.Graphite,
                                lineHeight = 20.sp
                            )
                        }
                        IconButton(
                            onClick = { onDeleteTopicClick(registeredTopic) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Eliminar tema registrado",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        if (profile.tutorName.isNotBlank() || profile.email.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = InstitutionalColors.SurfaceTint),
                    border = BorderStroke(1.dp, InstitutionalColors.Border),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenProfileClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SupervisedUserCircle,
                            null,
                            tint = InstitutionalColors.GreenDark,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            if (profile.tutorName.isNotBlank()) {
                                Text(
                                    "Tutor asignado: ${profile.tutorName}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = InstitutionalColors.Graphite
                                )
                            }
                            if (profile.email.isNotBlank()) {
                                Text(profile.email, fontSize = 12.sp, color = InstitutionalColors.MutedText)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, "Ver perfil", tint = InstitutionalColors.MutedText)
                    }
                }
            }
        }

        item { SectionTitle("Próximo paso", "Ver guía metodológica", onClick = onNavigateToGuide) }

        item {
            state.deliverables.firstOrNull { it.status != DeliverableStatus.APPROVED }?.let { next ->
                DeliverableCardItem(
                    item = next,
                    onStatusSelected = { status -> onDeliverableStatusChange(next.id, status) },
                    showActions = false
                )
            }
        }
    }
}
