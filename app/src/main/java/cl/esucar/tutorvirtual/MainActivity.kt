@file:OptIn(ExperimentalMaterial3Api::class)

package cl.esucar.tutorvirtual

import android.Manifest
import android.app.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import cl.esucar.tutorvirtual.data.*
import cl.esucar.tutorvirtual.ui.TutorViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val viewModel: TutorViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        scheduleDeadlineReminders()
        setContent { TutorApp(viewModel) }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("deadlines", "Recordatorios académicos", NotificationManager.IMPORTANCE_DEFAULT)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun scheduleDeadlineReminders() {
        val request = PeriodicWorkRequestBuilder<DeadlineReminderWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "deadline-reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

@Composable
fun TutorApp(vm: TutorViewModel) {
    val nav = rememberNavController()
    val currentDestination = nav.currentBackStackEntryAsState().value?.destination
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF005A8D), secondary = Color(0xFF00796B))) {
        Scaffold(
            topBar = { TopAppBar(title = { InstitutionalBrand() }) },
            bottomBar = { BottomBar(nav, currentDestination) }
        ) { padding ->
            NavHost(nav, startDestination = "dashboard", Modifier.padding(padding)) {
                composable("dashboard") { Dashboard(vm, nav) }
                composable("guide") { Guide(vm, nav) }
                composable("stage/{id}") { backStack ->
                    StageDetail(vm, vm.stages.first { it.id == backStack.arguments?.getString("id") }, nav)
                }
                composable("faq") { FaqScreen(vm) }
                composable("deliverables") { Deliverables(vm) }
            }
        }
    }
}

@Composable
private fun InstitutionalBrand() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(
            painter = painterResource(R.drawable.esucar_logo),
            contentDescription = "Emblema de la Escuela de Suboficiales S.O.M. Fabriciano González Urzúa",
            modifier = Modifier.size(42.dp),
        )
        Image(
            painter = painterResource(R.drawable.carabineros_logo),
            contentDescription = "Emblema de Carabineros de Chile",
            modifier = Modifier.size(width = 28.dp, height = 42.dp),
        )
        Column {
            Text("Tutor Virtual", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("ESUCAR · Carabineros de Chile", fontSize = 10.sp, color = Color.DarkGray)
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, currentDestination: androidx.navigation.NavDestination?) {
    NavigationBar {
        listOf("dashboard" to Icons.Default.Home to "Inicio", "guide" to Icons.Default.MenuBook to "Guía",
            "faq" to Icons.Default.Search to "FAQ", "deliverables" to Icons.Default.TaskAlt to "Hitos").forEach { (entry, label) ->
            NavigationBarItem(selected = currentDestination?.route == entry.first,
                onClick = { nav.navigate(entry.first) { launchSingleTop = true } },
                icon = { Icon(entry.second, label) }, label = { Text(label) })
        }
    }
}

@Composable
private fun Dashboard(vm: TutorViewModel, nav: NavHostController) {
    val deliverables by vm.deliverables.collectAsState()
    val progress by animateFloatAsState((deliverables.count { it.status == DeliverableStatus.APPROVED } / deliverables.size.coerceAtLeast(1)).toFloat(), label = "progress")
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            AnimatedVisibility(true, enter = fadeIn() + slideInVertically()) {
                Column {
                    Text("Hola, estudiante", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Avanza con claridad en tu Trabajo de Título.", color = Color.DarkGray)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F2F8)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Progreso general", fontWeight = FontWeight.Bold)
                    Text("${(progress * 100).toInt()}% de hitos aprobados", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth().height(10.dp))
                }
            }
        }
        item { SectionTitle("Próximo paso", "Ver guía metodológica") { nav.navigate("guide") } }
        item {
            val next = deliverables.firstOrNull { it.status != DeliverableStatus.APPROVED }
            if (next != null) DeliverableCard(next, vm)
        }
        item { SectionTitle("Accesos rápidos", null) {} }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("Guía", Icons.Default.MenuBook) { nav.navigate("guide") }
            QuickAction("Preguntas", Icons.Default.QuestionMark) { nav.navigate("faq") }
            QuickAction("Hitos", Icons.Default.TaskAlt) { nav.navigate("deliverables") }
        }}
    }
}

@Composable
private fun SectionTitle(title: String, action: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        if (action != null) Text(action, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onClick() })
    }
}

@Composable
private fun RowScope.QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(Modifier.weight(1f).clickable { onClick() }) { Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Text(label, fontSize = 12.sp)
    }}
}

@Composable
private fun Guide(vm: TutorViewModel, nav: NavHostController) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Guía metodológica", fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("Cinco etapas para ordenar tu investigación.") }
        items(vm.stages) { stage ->
            Card(Modifier.fillMaxWidth().clickable { nav.navigate("stage/${stage.id}") }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(Color(stage.color), RoundedCornerShape(6.dp)))
                    Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) {
                        Text(stage.title, fontWeight = FontWeight.Bold); Text(stage.description, maxLines = 2, color = Color.DarkGray)
                    }; Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
    }
}

@Composable
private fun StageDetail(vm: TutorViewModel, stage: Stage, nav: NavHostController) {
    val context = LocalContext.current
    val checklist by vm.checklist.collectAsState()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var documentName by remember { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap = it }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { documentName = it?.lastPathSegment }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) camera.launch(null) else Toast.makeText(context, "Se necesita permiso de cámara para registrar evidencia", Toast.LENGTH_SHORT).show()
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text(stage.title, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text(stage.description, fontSize = 17.sp) }
        item { AssistChip(onClick = {}, label = { Text("Formato requerido: ${stage.format}") }, leadingIcon = { Icon(Icons.Default.Description, null) }) }
        item { Text("Checklist", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        items(stage.checklist.indices.toList()) { index ->
            val check = stage.checklist[index]
            val done = checklist[stage.id].orEmpty().contains(index)
            Row(Modifier.fillMaxWidth().clickable { vm.setChecklistItem(stage.id, index, !done) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(done, { vm.setChecklistItem(stage.id, index, it) }); Text(check, modifier = Modifier.alpha(if (done) .55f else 1f))
        }}
        item {
            Card { Column(Modifier.padding(16.dp)) {
                Text("Recursos nativos", fontWeight = FontWeight.Bold)
                Text("Registra una evidencia con cámara o adjunta un documento.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) camera.launch(null)
                        else cameraPermission.launch(Manifest.permission.CAMERA)
                    }) { Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(6.dp)); Text("Cámara") }
                    OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf", "text/*", "image/*")) }) { Icon(Icons.Default.AttachFile, null); Spacer(Modifier.width(6.dp)); Text("Adjuntar") }
                }
                bitmap?.let { Image(it.asImageBitmap(), "Evidencia", Modifier.fillMaxWidth().height(150.dp)) }
                documentName?.let { Text("Documento seleccionado: $it", color = MaterialTheme.colorScheme.primary) }
            }}
        }
        item { TextButton(onClick = { nav.popBackStack() }) { Text("Volver a la guía") } }
    }
}

@Composable
private fun FaqScreen(vm: TutorViewModel) {
    var query by remember { mutableStateOf("") }; var category by remember { mutableStateOf("Todas") }
    val categories = listOf("Todas", "Metodología", "Citación APA", "Formato")
    val filtered = vm.faqs.filter { (category == "Todas" || it.category == category) && (query.isBlank() || it.question.contains(query, true) || it.answer.contains(query, true)) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Preguntas frecuentes", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Buscar por palabra clave") }, leadingIcon = { Icon(Icons.Default.Search, null) })
            Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { categories.forEach { FilterChip(selected = category == it, onClick = { category = it }, label = { Text(it) }) } }
        }
        items(filtered) { faq -> var expanded by remember { mutableStateOf(false) }; Card(Modifier.fillMaxWidth().clickable { expanded = !expanded }) { Column(Modifier.padding(16.dp)) {
            Text(faq.category.uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(faq.question, fontWeight = FontWeight.Bold)
            AnimatedVisibility(expanded) { Text(faq.answer, Modifier.padding(top = 8.dp), color = Color.DarkGray) }
        }}}
    }
}

@Composable
private fun Deliverables(vm: TutorViewModel) {
    val items by vm.deliverables.collectAsState(); var showAdd by remember { mutableStateOf(false) }; val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showDeadlineNotification(context)
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("Mis hitos", fontSize = 28.sp, fontWeight = FontWeight.Bold); FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Add, "Agregar") } } }
        items(items) { DeliverableCard(it, vm) }
        item {
            OutlinedButton(onClick = {
                if (android.os.Build.VERSION.SDK_INT >= 33 &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else showDeadlineNotification(context)
            }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Notifications, null); Spacer(Modifier.width(8.dp)); Text("Activar recordatorios")
            }
        }
    }
    if (showAdd) AddDeliverableDialog({ showAdd = false }, vm)
}

@Composable
private fun DeliverableCard(item: Deliverable, vm: TutorViewModel) {
    Card { Column(Modifier.padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(item.title, fontWeight = FontWeight.Bold); StatusBadge(item.status) }
        Text("Fecha límite: ${item.dueDate}", color = Color.DarkGray); Spacer(Modifier.height(8.dp))
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded, { expanded = !expanded }) { OutlinedTextField(item.status.label, {}, Modifier.menuAnchor().fillMaxWidth(), readOnly = true, label = { Text("Estado") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
            ExposedDropdownMenu(expanded, { expanded = false }) { DeliverableStatus.entries.forEach { status -> DropdownMenuItem(text = { Text(status.label) }, onClick = { vm.updateStatus(item.id, status); expanded = false }) } }
        }
    }}
}

@Composable
private fun StatusBadge(status: DeliverableStatus) {
    Surface(color = when (status) { DeliverableStatus.APPROVED -> Color(0xFFD8F3DC); DeliverableStatus.SUBMITTED -> Color(0xFFFFE8B6); else -> Color(0xFFE8EDF2) }, shape = RoundedCornerShape(20.dp)) { Text(status.label, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 12.sp) }
}

@Composable
private fun AddDeliverableDialog(onDismiss: () -> Unit, vm: TutorViewModel) {
    var title by remember { mutableStateOf("") }; var date by remember { mutableStateOf("") }; var submitted by remember { mutableStateOf(false) }
    val validTitle = title.trim().length >= 3; val validDate = date.trim().length >= 4
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nuevo hito") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Nombre del hito") }, isError = submitted && !validTitle, supportingText = { if (submitted && !validTitle) Text("Ingresa al menos 3 caracteres") }, leadingIcon = { Icon(Icons.Default.Title, null) })
        OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Fecha límite (ej. 20 dic 2026)") }, isError = submitted && !validDate, supportingText = { if (submitted && !validDate) Text("Indica una fecha para organizarte") }, leadingIcon = { Icon(Icons.Default.Event, null) })
    }}, confirmButton = { Button(onClick = { submitted = true; if (validTitle && validDate) { vm.addDeliverable(title.trim(), date.trim()); onDismiss() } }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

private fun showDeadlineNotification(context: android.content.Context) {
    if (android.os.Build.VERSION.SDK_INT >= 33 && ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(context, "Concede permiso de notificaciones para recibir recordatorios", Toast.LENGTH_SHORT).show(); return
    }
    val notification = NotificationCompat.Builder(context, "deadlines").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Tutor Virtual ESUCAR").setContentText("Recuerda revisar tu próximo hito académico.").setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
    NotificationManagerCompat.from(context).notify(1001, notification)
}
