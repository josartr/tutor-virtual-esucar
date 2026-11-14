@file:OptIn(ExperimentalMaterial3Api::class)

package cl.esucar.tutorvirtual.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.esucar.tutorvirtual.domain.model.Deliverable
import cl.esucar.tutorvirtual.domain.model.DeliverableStatus
import cl.esucar.tutorvirtual.domain.model.DeliverableUrgency
import cl.esucar.tutorvirtual.domain.model.calculateUrgency
import cl.esucar.tutorvirtual.ui.theme.InstitutionalColors

@Composable
fun SectionTitle(title: String, action: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        if (action != null) {
            Text(
                action,
                color = InstitutionalColors.Navy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onClick() }
            )
        }
    }
}

@Composable
fun RowScope.QuickAction(
    label: String,
    icon: ImageVector,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) InstitutionalColors.GreenSoft else InstitutionalColors.White
        ),
        border = BorderStroke(
            if (highlighted) 1.5.dp else 1.dp,
            if (highlighted) InstitutionalColors.Green.copy(alpha = 0.45f) else InstitutionalColors.Border
        )
    ) {
        Column(
            Modifier
                .padding(vertical = 12.dp, horizontal = 6.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                null,
                tint = if (highlighted) InstitutionalColors.GreenDark else InstitutionalColors.Green,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Medium,
                color = if (highlighted) InstitutionalColors.GreenDark else InstitutionalColors.Graphite,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DeliverableCardItem(
    item: Deliverable,
    onStatusSelected: (DeliverableStatus) -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    showActions: Boolean = true
) {
    val urgency = calculateUrgency(item.dueDate)

    Card(
        colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
        border = BorderStroke(1.dp, InstitutionalColors.Border),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("Fecha límite: ${item.dueDate}", color = InstitutionalColors.Graphite, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(item.status)
                    if (showActions) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, "Editar", tint = InstitutionalColors.Green, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    UrgencyBadge(urgency)
                }

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = item.status.label,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .width(156.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DeliverableStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.label) },
                                onClick = {
                                    onStatusSelected(status)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UrgencyBadge(urgency: DeliverableUrgency) {
    val (bgColor, textColor, label) = when (urgency) {
        DeliverableUrgency.EXPIRED -> Triple(Color(0xFFFBEAE9), Color(0xFF9F322C), "Vencido")
        DeliverableUrgency.URGENT -> Triple(Color(0xFFFCF2E4), Color(0xFF9E5A14), "Vence pronto (<= 3 días)")
        DeliverableUrgency.UPCOMING -> Triple(Color(0xFFEAF3EC), Color(0xFF1F5D3E), "Próximo (<= 7 días)")
        DeliverableUrgency.NORMAL -> Triple(Color(0xFFF1F4F2), Color(0xFF5D6962), "A tiempo")
    }

    Surface(color = bgColor, shape = RoundedCornerShape(12.dp)) {
        Text(
            label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatusBadge(status: DeliverableStatus) {
    Surface(
        color = when (status) {
            DeliverableStatus.APPROVED -> Color(0xFFE3F1E7)
            DeliverableStatus.SUBMITTED -> Color(0xFFFBF0DA)
            DeliverableStatus.IN_PROGRESS -> Color(0xFFE8F1E4)
            else -> Color(0xFFEDF1F4)
        },
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            status.label,
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = InstitutionalColors.GreenDark
        )
    }
}
