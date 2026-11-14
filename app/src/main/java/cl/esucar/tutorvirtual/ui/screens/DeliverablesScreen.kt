package cl.esucar.tutorvirtual.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.esucar.tutorvirtual.domain.model.Deliverable
import cl.esucar.tutorvirtual.domain.model.DeliverableStatus
import cl.esucar.tutorvirtual.ui.components.DeliverableCardItem
import cl.esucar.tutorvirtual.ui.theme.InstitutionalColors

@Composable
fun DeliverablesScreen(
    deliverables: List<Deliverable>,
    onAddClick: () -> Unit,
    onEditClick: (Deliverable) -> Unit,
    onDeleteClick: (Deliverable) -> Unit,
    onStatusSelected: (String, DeliverableStatus) -> Unit,
    onActivateRemindersClick: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Mis hitos académicos", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = InstitutionalColors.Green,
                    contentColor = InstitutionalColors.White,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Add, "Agregar Hito")
                }
            }
        }

        items(deliverables, key = { it.id }) { item ->
            DeliverableCardItem(
                item = item,
                onStatusSelected = { status -> onStatusSelected(item.id, status) },
                onEdit = { onEditClick(item) },
                onDelete = { onDeleteClick(item) }
            )
        }

        item {
            OutlinedButton(
                onClick = onActivateRemindersClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Notifications, null)
                Spacer(Modifier.width(8.dp))
                Text("Activar recordatorios periódicos")
            }
        }
    }
}
