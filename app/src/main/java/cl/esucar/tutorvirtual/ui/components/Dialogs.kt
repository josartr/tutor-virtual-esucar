@file:OptIn(ExperimentalMaterial3Api::class)

package cl.esucar.tutorvirtual.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.esucar.tutorvirtual.domain.model.Deliverable
import cl.esucar.tutorvirtual.domain.model.ProjectTopic
import cl.esucar.tutorvirtual.domain.model.UserRole
import cl.esucar.tutorvirtual.ui.state.DeliverableEditorUiState
import cl.esucar.tutorvirtual.ui.state.ProfileEditorUiState
import cl.esucar.tutorvirtual.ui.state.TopicRegistryUiState
import cl.esucar.tutorvirtual.ui.theme.InstitutionalColors

@Composable
fun ProfileDialog(
    state: ProfileEditorUiState,
    onRoleSelected: (UserRole) -> Unit,
    onRankSelected: (String) -> Unit,
    onFirstNameChanged: (String) -> Unit,
    onFirstLastNameChanged: (String) -> Unit,
    onSecondLastNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onTutorSelected: (String) -> Unit,
    onSave: () -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isOpen) return

    var rankExpanded by remember { mutableStateOf(false) }
    var tutorExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Perfil · ${state.userRole.label}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = InstitutionalColors.GreenSoft,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            null,
                            tint = InstitutionalColors.GreenDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Autenticado con: ${state.ssoProvider}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = InstitutionalColors.GreenDark
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        UserRole.ESTUDIANTE to Icons.Default.School,
                        UserRole.FUNCIONARIO to Icons.Default.Badge
                    ).forEach { (roleOption, icon) ->
                        val selected = state.userRole == roleOption
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) InstitutionalColors.GreenSoft else InstitutionalColors.Background,
                            border = BorderStroke(
                                if (selected) 1.5.dp else 1.dp,
                                if (selected) InstitutionalColors.Green else InstitutionalColors.Border
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onRoleSelected(roleOption) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (selected) InstitutionalColors.GreenDark else InstitutionalColors.MutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    roleOption.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) InstitutionalColors.GreenDark else InstitutionalColors.Graphite
                                )
                            }
                        }
                    }
                }

                if (state.showPoliceRank) {
                    ExposedDropdownMenuBox(
                        expanded = rankExpanded,
                        onExpandedChange = { rankExpanded = !rankExpanded }
                    ) {
                        OutlinedTextField(
                            value = state.rank,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Grado policial") },
                            leadingIcon = { Icon(Icons.Default.Security, null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(rankExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = rankExpanded,
                            onDismissRequest = { rankExpanded = false }
                        ) {
                            state.availableRanks.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r) },
                                    onClick = {
                                        onRankSelected(r)
                                        rankExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = onFirstNameChanged,
                    label = { Text("Nombres") },
                    singleLine = true,
                    isError = state.submitted && !state.validFirstName,
                    supportingText = {
                        if (state.submitted && !state.validFirstName) Text("Ingresa al menos 2 caracteres")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.firstLastName,
                        onValueChange = onFirstLastNameChanged,
                        label = { Text("1er Apellido") },
                        singleLine = true,
                        isError = state.submitted && !state.validFirstLastName,
                        supportingText = {
                            if (state.submitted && !state.validFirstLastName) Text("Mín. 2 letras")
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state.secondLastName,
                        onValueChange = onSecondLastNameChanged,
                        label = { Text("2do Apellido") },
                        singleLine = true,
                        isError = state.submitted && !state.validSecondLastName,
                        supportingText = {
                            if (state.submitted && !state.validSecondLastName) Text("Mín. 2 letras")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = state.email,
                    onValueChange = onEmailChanged,
                    label = { Text("Correo Institucional") },
                    singleLine = true,
                    isError = state.submitted && !state.validEmail,
                    supportingText = {
                        if (state.submitted && !state.validEmail) {
                            Text("Usa un correo @esucar.cl o @carabineros.cl")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.researchTopic.isNotBlank()) {
                    OutlinedTextField(
                        value = state.researchTopic,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tema registrado (no modificable)") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        maxLines = 2,
                        supportingText = { Text("Para añadir o eliminar temas usa el registro en Inicio.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = tutorExpanded,
                    onExpandedChange = { tutorExpanded = !tutorExpanded }
                ) {
                    OutlinedTextField(
                        value = state.tutorName,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Profesor Tutor ESUCAR") },
                        leadingIcon = { Icon(Icons.Default.SupervisedUserCircle, null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tutorExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = tutorExpanded,
                        onDismissRequest = { tutorExpanded = false }
                    ) {
                        state.availableTutors.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t, fontSize = 12.sp) },
                                onClick = {
                                    onTutorSelected(t)
                                    tutorExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = InstitutionalColors.Green)
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onLogout) {
                Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
fun AddTopicDialog(
    state: TopicRegistryUiState,
    onTopicTextChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isAddDialogOpen) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir tema de proyecto", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Una vez registrado, este tema queda guardado de forma permanente y no podrá modificarse (solo eliminarse).",
                    fontSize = 12.sp,
                    color = InstitutionalColors.MutedText
                )
                OutlinedTextField(
                    value = state.newTopicText,
                    onValueChange = onTopicTextChanged,
                    label = { Text("Título del tema de investigación") },
                    maxLines = 3,
                    isError = state.submitted && !state.validNewTopic,
                    supportingText = {
                        if (state.submitted && !state.validNewTopic) Text("Ingresa al menos 4 caracteres")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = InstitutionalColors.Green)
            ) {
                Text("Registrar tema")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun DeleteTopicDialog(
    topic: ProjectTopic?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (topic == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar tema registrado") },
        text = { Text("¿Deseas eliminar '${topic.title}' del registro de temas?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun AddEditDeliverableDialog(
    state: DeliverableEditorUiState,
    onTitleChanged: (String) -> Unit,
    onDueDateChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isEditorOpen) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.editingItem == null) "Nuevo Hito Académico" else "Editar Hito") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = onTitleChanged,
                    label = { Text("Nombre del Hito") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.submitted && !state.validTitle,
                    supportingText = {
                        if (state.submitted && !state.validTitle) Text("Ingresa al menos 3 caracteres")
                    }
                )
                OutlinedTextField(
                    value = state.dueDate,
                    onValueChange = onDueDateChanged,
                    label = { Text("Fecha límite (ej. 20 dic 2026)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.submitted && !state.validDate,
                    supportingText = {
                        if (state.submitted && !state.validDate) Text("Indica la fecha de entrega")
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = InstitutionalColors.Green)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun DeleteDeliverableDialog(
    item: Deliverable?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (item == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar Hito") },
        text = { Text("¿Deseas eliminar el hito '${item.title}'?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
