package cl.esucar.tutorvirtual.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import cl.esucar.tutorvirtual.domain.model.Stage
import cl.esucar.tutorvirtual.ui.theme.InstitutionalColors

@Composable
fun GuideScreen(
    stages: List<Stage>,
    checklist: Map<String, Set<Int>>,
    onStageClick: (String) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Guía metodológica", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Cinco etapas estructuradas para tu Trabajo de Título.")
        }

        items(stages, key = { it.id }) { stage ->
            val completedCount = checklist[stage.id].orEmpty().count { it in stage.checklist.indices }
            val totalCount = stage.checklist.size
            val stageProgress = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStageClick(stage.id) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                border = BorderStroke(1.dp, InstitutionalColors.Border)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .background(Color(stage.color), RoundedCornerShape(6.dp))
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stage.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(stage.description, maxLines = 2, color = InstitutionalColors.Graphite, fontSize = 13.sp)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = InstitutionalColors.Graphite)
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("$completedCount de $totalCount tareas completadas", fontSize = 12.sp, color = InstitutionalColors.Graphite)
                        Surface(
                            color = if (completedCount == totalCount) InstitutionalColors.GreenSoft else Color(0xFFF0F0F0),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                if (completedCount == totalCount) "Completada" else "${(stageProgress * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (completedCount == totalCount) InstitutionalColors.GreenDark else InstitutionalColors.Graphite,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { stageProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(stage.color),
                        trackColor = Color(stage.color).copy(alpha = 0.15f)
                    )
                }
            }
        }
    }
}

@Composable
fun StageDetailScreen(
    stage: Stage,
    completedIndices: Set<Int>,
    note: String,
    onChecklistToggle: (Int, Boolean) -> Unit,
    onNoteChanged: (String) -> Unit,
    onPermissionDeniedMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var documentName by remember { mutableStateOf<String?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap = it }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        documentName = it?.lastPathSegment
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) camera.launch(null)
        else onPermissionDeniedMessage("Se necesita permiso de cámara para registrar evidencia")
    }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(stage.title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(stage.description, fontSize = 16.sp, color = InstitutionalColors.Graphite)
        }

        item {
            AssistChip(
                onClick = {},
                label = { Text("Formato requerido: ${stage.format}") },
                leadingIcon = { Icon(Icons.Default.Description, null) }
            )
        }

        item { Text("Checklist de la Etapa", fontSize = 20.sp, fontWeight = FontWeight.Bold) }

        items(stage.checklist.indices.toList()) { index ->
            val check = stage.checklist[index]
            val done = completedIndices.contains(index)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onChecklistToggle(index, !done) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = done, onCheckedChange = { onChecklistToggle(index, it) })
                Text(check, modifier = Modifier.alpha(if (done) 0.55f else 1f))
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                border = BorderStroke(1.dp, InstitutionalColors.Border),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bitácora del Estudiante / Apuntes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "Escribe aquí tus dudas para tu Profesor Tutor o ideas de tu avance. Se guarda automáticamente.",
                        fontSize = 12.sp,
                        color = InstitutionalColors.Graphite
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = onNoteChanged,
                        placeholder = { Text("Ej. Consultar con tutor sobre la matriz de datos...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 4
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                border = BorderStroke(1.dp, InstitutionalColors.Border),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Evidencias del Trabajo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Registra evidencias con la cámara o adjunta archivos de avance.", fontSize = 13.sp, color = InstitutionalColors.Graphite)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    camera.launch(null)
                                } else {
                                    cameraPermission.launch(Manifest.permission.CAMERA)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = InstitutionalColors.Green)
                        ) {
                            Icon(Icons.Default.CameraAlt, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Cámara")
                        }

                        OutlinedButton(
                            onClick = { picker.launch(arrayOf("application/pdf", "text/*", "image/*")) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = InstitutionalColors.Navy)
                        ) {
                            Icon(Icons.Default.AttachFile, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Adjuntar")
                        }
                    }
                    bitmap?.let {
                        Image(
                            it.asImageBitmap(),
                            "Evidencia",
                            Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    documentName?.let { Text("Documento seleccionado: $it", color = InstitutionalColors.Navy, fontSize = 13.sp) }
                }
            }
        }

        item {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Volver a la guía")
            }
        }
    }
}
