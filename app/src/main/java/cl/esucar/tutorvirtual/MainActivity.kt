@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package cl.esucar.tutorvirtual

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import cl.esucar.tutorvirtual.data.*
import cl.esucar.tutorvirtual.ui.InstitutionalColors
import cl.esucar.tutorvirtual.ui.InstitutionalLightColors
import cl.esucar.tutorvirtual.ui.RadioPlaybackState
import cl.esucar.tutorvirtual.ui.TutorViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val viewModel: TutorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    val userProfile by vm.userProfile.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val loadingMessage by vm.loadingMessage.collectAsState()
    var showProfileDialog by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = InstitutionalLightColors) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!userProfile.isLoggedIn) {
                CleanLoginScreen(vm)
            } else {
                val nav = rememberNavController()
                val currentDestination = nav.currentBackStackEntryAsState().value?.destination

                Scaffold(
                    containerColor = InstitutionalColors.Background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        TopAppBar(
                            title = { InstitutionalBrand() },
                            actions = {
                                IconButton(onClick = { showProfileDialog = true }) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(InstitutionalColors.White.copy(alpha = 0.18f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = "Perfil de usuario",
                                            tint = InstitutionalColors.White
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = InstitutionalColors.Green,
                                titleContentColor = InstitutionalColors.White
                            )
                        )
                    },
                    bottomBar = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MiniRadioBar(vm)
                            BottomBar(nav, currentDestination)
                        }
                    }
                ) { padding ->
                    NavHost(nav, startDestination = "dashboard", Modifier.padding(padding)) {
                        composable("dashboard") { Dashboard(vm, nav, onOpenProfile = { showProfileDialog = true }) }
                        composable("guide") { Guide(vm, nav) }
                        composable("stage/{id}") { backStack ->
                            val stageId = backStack.arguments?.getString("id")
                            val stage = vm.stages.firstOrNull { it.id == stageId } ?: vm.stages.first()
                            StageDetail(vm, stage, nav)
                        }
                        composable("study") { StudyToolsScreen(vm) }
                        composable("faq") { FaqScreen(vm) }
                        composable("deliverables") { Deliverables(vm) }
                        composable("news") { NewsScreen(vm) }
                    }
                }

                if (showProfileDialog) {
                    ProfileDialog(userProfile, onDismiss = { showProfileDialog = false }, vm = vm)
                }
            }

            GlobalLoadingOverlay(
                visible = isLoading,
                message = loadingMessage
            )
        }
    }
}

@Composable
private fun GlobalLoadingOverlay(visible: Boolean, message: String) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF15221B).copy(alpha = 0.34f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {},
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = InstitutionalColors.White,
                border = BorderStroke(1.dp, InstitutionalColors.Border),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .widthIn(min = 220.dp, max = 300.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(
                        color = InstitutionalColors.Green,
                        trackColor = InstitutionalColors.GreenSoft,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = InstitutionalColors.Graphite,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

private fun openExternalUrl(
    context: Context,
    url: String,
    vm: TutorViewModel,
    loadingText: String = "Abriendo en el navegador..."
) {
    vm.runWithGlobalLoading(loadingText, minDurationMs = 260L) {
        val launched = runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.isSuccess
        if (!launched) {
            Toast.makeText(
                context,
                "No se encontró un navegador disponible para abrir el enlace.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@Composable
private fun CleanLoginScreen(vm: TutorViewModel) {
    var registrationMode by remember { mutableStateOf(false) }
    var userRole by remember { mutableStateOf(UserRole.ESTUDIANTE) }
    var emailAutoManaged by remember { mutableStateOf(true) }
    var rank by remember { mutableStateOf(defaultRanks[1]) }
    var rankExpanded by remember { mutableStateOf(false) }
    var tutor by remember { mutableStateOf(defaultEsucarTutors.first()) }
    var tutorExpanded by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }
    var firstLastName by remember { mutableStateOf("") }
    var secondLastName by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }

    val detectedInstitution = institutionForEmail(email)
    val validEmail = detectedInstitution != null
    val validPassword = password.length >= 8
    val passwordsMatch = password == confirmPassword
    val validFirstName = firstName.trim().length >= 2
    val validFirstLastName = firstLastName.trim().length >= 2
    val validSecondLastName = secondLastName.trim().length >= 2
    val validTopic = topic.trim().length >= 4
    val isCarabinerosDomain = detectedInstitution == InstitutionType.CARABINEROS ||
        (registrationMode && userRole == UserRole.FUNCIONARIO)
    val validRank = !isCarabinerosDomain || rank.isNotBlank()

    val isLoading by vm.isLoading.collectAsState()

    fun syncAutoEmail(
        nextFirst: String = firstName,
        nextLast1: String = firstLastName,
        nextLast2: String = secondLastName,
        nextRole: UserRole = userRole,
        forceRegenerate: Boolean = false
    ) {
        if (!registrationMode) return
        if (forceRegenerate || emailAutoManaged) {
            emailAutoManaged = true
            email = generateInstitutionalEmail(nextFirst, nextLast1, nextLast2, nextRole)
        } else {
            val localPart = email.trim().substringBefore("@")
            if (localPart.isNotBlank()) {
                val domain = if (nextRole == UserRole.FUNCIONARIO) "carabineros.cl" else "esucar.cl"
                email = "$localPart@$domain"
            }
        }
    }

    fun applyDomainSuggestion(domain: String) {
        val localPart = email.trim().substringBefore("@").ifBlank { "nombre.apellido" }
        email = "$localPart@$domain"
        if (registrationMode) {
            emailAutoManaged = false
            userRole = if (domain == "carabineros.cl") UserRole.FUNCIONARIO else UserRole.ESTUDIANTE
        }
    }

    fun submit() {
        submitted = true
        message = null
        messageIsError = false
        if (!validEmail || !validPassword) return

        if (!registrationMode) {
            vm.loginAsync(email, password) { result ->
                when (result) {
                    is AuthResult.Success -> Unit
                    AuthResult.InvalidCredentials -> {
                        message = "Correo o contraseña incorrectos."
                        messageIsError = true
                    }
                    AuthResult.EmailAlreadyRegistered -> Unit
                    AuthResult.InvalidRegistration -> Unit
                    AuthResult.PersistenceError -> {
                        message = "No fue posible acceder a la base de datos. Intenta nuevamente."
                        messageIsError = true
                    }
                }
            }
            return
        }

        val resolvedInstitution = detectedInstitution ?: return
        if (!validFirstName || !validFirstLastName || !validSecondLastName || !validTopic || !validRank || !passwordsMatch) return
        vm.registerUserAsync(
            rank = resolveProfileRank(resolvedInstitution, userRole, rank),
            firstName = firstName,
            firstLastName = firstLastName,
            secondLastName = secondLastName,
            email = email,
            researchTopic = topic,
            tutorName = tutor,
            institutionType = resolvedInstitution,
            userRole = userRole,
            password = password
        ) { result ->
            when (result) {
                is AuthResult.Success -> Unit
                AuthResult.EmailAlreadyRegistered -> {
                    message = "Este correo ya está registrado. Inicia sesión para continuar."
                    messageIsError = true
                }
                AuthResult.InvalidCredentials -> Unit
                AuthResult.InvalidRegistration -> {
                    message = "Revisa los datos del registro."
                    messageIsError = true
                }
                AuthResult.PersistenceError -> {
                    message = "No fue posible guardar la cuenta. Intenta nuevamente."
                    messageIsError = true
                }
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = InstitutionalColors.Background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                    border = BorderStroke(1.dp, InstitutionalColors.Green.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.carabineros_banner),
                            contentDescription = "Carabineros de Chile · Orden y Patria",
                            colorFilter = ColorFilter.colorMatrix(
                                ColorMatrix(
                                    floatArrayOf(
                                        -0.9702f, 0f, 0f, 0f, 269.55f,
                                        0f, -0.6851f, 0f, 0f, 265.28f,
                                        0f, 0f, -0.8170f, 0f, 267.26f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "ESCUELA DE SUBOFICIALES · ESUCAR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = InstitutionalColors.Green
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Tutor Virtual ESUCAR",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = InstitutionalColors.GreenDark
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Acceso unificado @esucar.cl y @carabineros.cl",
                    fontSize = 13.sp,
                    color = InstitutionalColors.MutedText,
                    textAlign = TextAlign.Center
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                    border = BorderStroke(1.dp, InstitutionalColors.Border),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = InstitutionalColors.Background,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(4.dp)) {
                                listOf(false to "Iniciar sesión", true to "Crear cuenta").forEach { (isReg, label) ->
                                    val selected = registrationMode == isReg
                                    Surface(
                                        modifier = Modifier.weight(1f).clickable {
                                            registrationMode = isReg
                                            submitted = false
                                            message = null
                                            if (isReg && email.isBlank() && (firstName.isNotBlank() || firstLastName.isNotBlank())) {
                                                syncAutoEmail(forceRegenerate = true)
                                            }
                                        },
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (selected) InstitutionalColors.Green else Color.Transparent
                                    ) {
                                        Box(
                                            Modifier.padding(vertical = 9.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                label,
                                                fontSize = 13.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) InstitutionalColors.White else InstitutionalColors.Graphite
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (registrationMode) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "¿Ingresas como estudiante o funcionario?",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = InstitutionalColors.GreenDark
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        Triple(UserRole.ESTUDIANTE, Icons.Default.School, "@esucar.cl"),
                                        Triple(UserRole.FUNCIONARIO, Icons.Default.Badge, "@carabineros.cl")
                                    ).forEach { (roleOption, icon, roleDomain) ->
                                        val selected = userRole == roleOption
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selected) InstitutionalColors.GreenSoft else InstitutionalColors.Background,
                                            border = BorderStroke(
                                                if (selected) 1.5.dp else 1.dp,
                                                if (selected) InstitutionalColors.Green else InstitutionalColors.Border
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    userRole = roleOption
                                                    syncAutoEmail(nextRole = roleOption)
                                                }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Row(
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
                                                        fontSize = 13.sp,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (selected) InstitutionalColors.GreenDark else InstitutionalColors.Graphite
                                                    )
                                                }
                                                Text(
                                                    roleDomain,
                                                    fontSize = 10.sp,
                                                    color = if (selected) InstitutionalColors.Green else InstitutionalColors.MutedText
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(visible = isCarabinerosDomain) {
                                ExposedDropdownMenuBox(
                                    expanded = rankExpanded,
                                    onExpandedChange = { rankExpanded = !rankExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = rank,
                                        onValueChange = {},
                                        readOnly = true,
                                        singleLine = true,
                                        label = { Text("Grado policial") },
                                        leadingIcon = { Icon(Icons.Default.Security, null) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(rankExpanded) },
                                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                        isError = submitted && !validRank
                                    )
                                    ExposedDropdownMenu(
                                        expanded = rankExpanded,
                                        onDismissRequest = { rankExpanded = false }
                                    ) {
                                        defaultRanks.drop(1).forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    rank = option
                                                    rankExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { next ->
                                    firstName = next
                                    syncAutoEmail(nextFirst = next)
                                },
                                label = { Text("Nombres") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                isError = submitted && !validFirstName,
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                supportingText = {
                                    if (submitted && !validFirstName) Text("Ingresa al menos 2 caracteres")
                                }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = firstLastName,
                                    onValueChange = { next ->
                                        firstLastName = next
                                        syncAutoEmail(nextLast1 = next)
                                    },
                                    label = { Text("1er Apellido") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    isError = submitted && !validFirstLastName,
                                    supportingText = {
                                        if (submitted && !validFirstLastName) Text("Obligatorio")
                                    }
                                )
                                OutlinedTextField(
                                    value = secondLastName,
                                    onValueChange = { next ->
                                        secondLastName = next
                                        syncAutoEmail(nextLast2 = next)
                                    },
                                    label = { Text("2do Apellido") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    isError = submitted && !validSecondLastName,
                                    supportingText = {
                                        if (submitted && !validSecondLastName) Text("Obligatorio")
                                    }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { next ->
                                email = next
                                if (registrationMode) {
                                    emailAutoManaged = next.isBlank()
                                    when (institutionForEmail(next)) {
                                        InstitutionType.CARABINEROS -> userRole = UserRole.FUNCIONARIO
                                        InstitutionType.ESUCAR -> userRole = UserRole.ESTUDIANTE
                                        null -> Unit
                                    }
                                }
                            },
                            label = { Text("Correo institucional") },
                            placeholder = {
                                Text(
                                    if (!registrationMode) {
                                        "usuario@esucar.cl o @carabineros.cl"
                                    } else if (userRole == UserRole.FUNCIONARIO) {
                                        "nombre.apellido@carabineros.cl"
                                    } else {
                                        "nombre.apellido@esucar.cl"
                                    }
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            isError = submitted && !validEmail,
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (registrationMode && !emailAutoManaged &&
                                        (firstName.isNotBlank() || firstLastName.isNotBlank())
                                    ) {
                                        IconButton(
                                            onClick = { syncAutoEmail(forceRegenerate = true) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.AutoFixHigh,
                                                contentDescription = "Sugerir correo automático",
                                                tint = InstitutionalColors.Navy,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    if (validEmail) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Dominio válido",
                                            tint = InstitutionalColors.Green,
                                            modifier = Modifier.padding(end = 10.dp)
                                        )
                                    }
                                }
                            },
                            supportingText = {
                                when {
                                    submitted && !validEmail -> Text("Usa un correo @esucar.cl o @carabineros.cl")
                                    registrationMode && emailAutoManaged && validEmail -> Text(
                                        "Generado automáticamente según tus datos (${userRole.label})",
                                        color = InstitutionalColors.GreenDark
                                    )
                                    detectedInstitution == InstitutionType.ESUCAR -> Text(
                                        "Dominio detectado: ESUCAR (@esucar.cl)",
                                        color = InstitutionalColors.GreenDark
                                    )
                                    detectedInstitution == InstitutionType.CARABINEROS -> Text(
                                        "Dominio detectado: Carabineros de Chile (@carabineros.cl)",
                                        color = InstitutionalColors.GreenDark
                                    )
                                    else -> Unit
                                }
                            }
                        )

                        if (!registrationMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("esucar.cl" to Icons.Default.School, "carabineros.cl" to Icons.Default.Security)
                                    .forEach { (domain, icon) ->
                                        val active = email.trim().lowercase().endsWith("@$domain")
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (active) InstitutionalColors.GreenSoft else InstitutionalColors.Background,
                                            border = BorderStroke(
                                                1.dp,
                                                if (active) InstitutionalColors.Green else InstitutionalColors.Border
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { applyDomainSuggestion(domain) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    icon,
                                                    contentDescription = null,
                                                    tint = if (active) InstitutionalColors.GreenDark else InstitutionalColors.MutedText,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(5.dp))
                                                Text(
                                                    "@$domain",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                                    color = if (active) InstitutionalColors.GreenDark else InstitutionalColors.Graphite,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                            }
                        }

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            isError = submitted && !validPassword,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                    )
                                }
                            },
                            supportingText = {
                                if (submitted && !validPassword) Text("Debe tener al menos 8 caracteres")
                            }
                        )

                        if (registrationMode) {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Repetir contraseña") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                isError = submitted && !passwordsMatch,
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = { Icon(Icons.Default.Lock, null) },
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (confirmPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                        )
                                    }
                                },
                                supportingText = {
                                    if (submitted && !passwordsMatch) Text("Las contraseñas no coinciden")
                                }
                            )

                            OutlinedTextField(
                                value = topic,
                                onValueChange = { topic = it },
                                label = { Text("Tema inicial del proyecto") },
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth(),
                                isError = submitted && !validTopic,
                                leadingIcon = { Icon(Icons.Default.Book, null) },
                                supportingText = {
                                    if (submitted && !validTopic) Text("Ingresa el tema de tu investigación")
                                }
                            )
                            ExposedDropdownMenuBox(
                                expanded = tutorExpanded,
                                onExpandedChange = { tutorExpanded = !tutorExpanded }
                            ) {
                                OutlinedTextField(
                                    value = tutor,
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    label = { Text("Profesor tutor") },
                                    leadingIcon = { Icon(Icons.Default.SupervisedUserCircle, null) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tutorExpanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = tutorExpanded,
                                    onDismissRequest = { tutorExpanded = false }
                                ) {
                                    defaultEsucarTutors.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option, fontSize = 12.sp) },
                                            onClick = {
                                                tutor = option
                                                tutorExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (message != null) {
                            Surface(
                                color = if (messageIsError) Color(0xFFFFEBEE) else InstitutionalColors.GreenSoft,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (messageIsError) Icons.Default.ErrorOutline else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (messageIsError) MaterialTheme.colorScheme.error else InstitutionalColors.GreenDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        message.orEmpty(),
                                        color = if (messageIsError) MaterialTheme.colorScheme.error else InstitutionalColors.GreenDark,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = ::submit,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = InstitutionalColors.Green)
                        ) {
                            Icon(
                                if (registrationMode) Icons.Default.PersonAdd else Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (registrationMode) "Crear cuenta institucional" else "Iniciar sesión",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { vm.loginQuickSSOAsync("SSO Institucional (Demo)") },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, InstitutionalColors.Border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = InstitutionalColors.Navy),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Vista previa rápida (Modo Demo)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = InstitutionalColors.MutedText,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Acceso seguro · Base de datos local cifrada PBKDF2",
                            fontSize = 11.sp,
                            color = InstitutionalColors.MutedText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDialog(profile: UserProfile, onDismiss: () -> Unit, vm: TutorViewModel) {
    val fallbackParts = profile.fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    var userRole by remember { mutableStateOf(profile.userRole) }
    var rank by remember { mutableStateOf(if (profile.rank in defaultRanks.drop(1)) profile.rank else defaultRanks[1]) }
    var rankExpanded by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf(profile.firstName.ifBlank { fallbackParts.firstOrNull().orEmpty() }) }
    var firstLastName by remember { mutableStateOf(profile.firstLastName.ifBlank { fallbackParts.getOrNull(1).orEmpty() }) }
    var secondLastName by remember { mutableStateOf(profile.secondLastName.ifBlank { fallbackParts.drop(2).joinToString(" ") }) }
    var email by remember { mutableStateOf(profile.email) }
    var tutor by remember { mutableStateOf(if (profile.tutorName.isBlank()) defaultEsucarTutors.first() else profile.tutorName) }
    var tutorExpanded by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val detectedInstitution = institutionForEmail(email) ?: profile.institutionType
    val validFirstName = firstName.trim().length >= 2
    val validFirstLastName = firstLastName.trim().length >= 2
    val validSecondLastName = secondLastName.trim().length >= 2
    val validEmail = isValidInstitutionalEmail(email)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Perfil · ${userRole.label}", fontWeight = FontWeight.Bold) },
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
                        Icon(Icons.Default.VerifiedUser, null, tint = InstitutionalColors.GreenDark, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Autenticado con: ${profile.ssoProvider}",
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
                        val selected = userRole == roleOption
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) InstitutionalColors.GreenSoft else InstitutionalColors.Background,
                            border = BorderStroke(
                                if (selected) 1.5.dp else 1.dp,
                                if (selected) InstitutionalColors.Green else InstitutionalColors.Border
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { userRole = roleOption }
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

                if (detectedInstitution == InstitutionType.CARABINEROS) {
                    ExposedDropdownMenuBox(expanded = rankExpanded, onExpandedChange = { rankExpanded = !rankExpanded }) {
                        OutlinedTextField(
                            value = rank,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Grado policial") },
                            leadingIcon = { Icon(Icons.Default.Security, null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(rankExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = rankExpanded, onDismissRequest = { rankExpanded = false }) {
                            defaultRanks.drop(1).forEach { r ->
                                DropdownMenuItem(text = { Text(r) }, onClick = { rank = r; rankExpanded = false })
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Nombres") },
                    singleLine = true,
                    isError = submitted && !validFirstName,
                    supportingText = { if (submitted && !validFirstName) Text("Ingresa al menos 2 caracteres") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = firstLastName,
                        onValueChange = { firstLastName = it },
                        label = { Text("1er Apellido") },
                        singleLine = true,
                        isError = submitted && !validFirstLastName,
                        supportingText = { if (submitted && !validFirstLastName) Text("Mín. 2 letras") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = secondLastName,
                        onValueChange = { secondLastName = it },
                        label = { Text("2do Apellido") },
                        singleLine = true,
                        isError = submitted && !validSecondLastName,
                        supportingText = { if (submitted && !validSecondLastName) Text("Mín. 2 letras") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Institucional") },
                    singleLine = true,
                    isError = submitted && !validEmail,
                    supportingText = { if (submitted && !validEmail) Text("Usa un correo @esucar.cl o @carabineros.cl") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (profile.researchTopic.isNotBlank()) {
                    OutlinedTextField(
                        value = profile.researchTopic,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tema registrado (no modificable)") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        maxLines = 2,
                        supportingText = { Text("Para añadir o eliminar temas usa el registro en Inicio.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ExposedDropdownMenuBox(expanded = tutorExpanded, onExpandedChange = { tutorExpanded = !tutorExpanded }) {
                    OutlinedTextField(
                        value = tutor,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Profesor Tutor ESUCAR") },
                        leadingIcon = { Icon(Icons.Default.SupervisedUserCircle, null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tutorExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = tutorExpanded, onDismissRequest = { tutorExpanded = false }) {
                        defaultEsucarTutors.forEach { t ->
                            DropdownMenuItem(text = { Text(t, fontSize = 12.sp) }, onClick = { tutor = t; tutorExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    submitted = true
                    if (validFirstName && validFirstLastName && validSecondLastName && validEmail) {
                        vm.updateUserProfileAsync(
                            rank = resolveProfileRank(detectedInstitution, userRole, rank),
                            firstName = firstName.trim(),
                            firstLastName = firstLastName.trim(),
                            secondLastName = secondLastName.trim(),
                            email = email.trim(),
                            tutorName = tutor,
                            userRole = userRole
                        ) {
                            onDismiss()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = InstitutionalColors.Green)
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                vm.logoutUser()
                onDismiss()
            }) {
                Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
private fun InstitutionalBrand() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(R.drawable.institutional_header_logo),
            contentDescription = "Emblema Carabineros de Chile ESUCAR",
            modifier = Modifier.height(36.dp).width(32.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "Tutor Virtual ESUCAR",
                color = InstitutionalColors.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Escuela de Suboficiales de Carabineros",
                color = InstitutionalColors.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MiniRadioBar(vm: TutorViewModel) {
    val radioState by vm.radioState.collectAsState()
    val radioError by vm.radioError.collectAsState()
    val radioVolume by vm.radioVolume.collectAsState()
    var lastNonZeroVolume by remember { mutableFloatStateOf(0.85f) }
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
                        color = if (radioState == RadioPlaybackState.PLAYING) InstitutionalColors.Green else InstitutionalColors.Navy,
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
                                modifier = Modifier.padding(horizontal = 8.dp).size(20.dp),
                                strokeWidth = 2.dp,
                                color = InstitutionalColors.Navy
                            )
                        }
                        RadioPlaybackState.ERROR -> {
                            IconButton(
                                onClick = { vm.playRadio() },
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
                                onClick = { vm.pauseRadio() },
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
                                onClick = { vm.resumeRadio() },
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
                        onClick = { vm.stopRadio() },
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
                            onClick = {
                                if (radioVolume > 0f) {
                                    lastNonZeroVolume = radioVolume
                                    vm.setRadioVolume(0f)
                                } else {
                                    vm.setRadioVolume(if (lastNonZeroVolume > 0.05f) lastNonZeroVolume else 0.85f)
                                }
                            },
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
                            onValueChange = {
                                if (it > 0.01f) lastNonZeroVolume = it
                                vm.setRadioVolume(it)
                            },
                            modifier = Modifier.weight(1f).height(26.dp),
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

@Composable
private fun BottomBar(nav: NavHostController, currentDestination: androidx.navigation.NavDestination?) {
    Surface(
        color = InstitutionalColors.White,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 1.dp, color = InstitutionalColors.Border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "dashboard" to Icons.Default.Home to "Inicio",
                    "guide" to Icons.AutoMirrored.Filled.MenuBook to "Guía",
                    "study" to Icons.Default.Timer to "Estudio",
                    "faq" to Icons.Default.Search to "FAQ",
                    "deliverables" to Icons.Default.TaskAlt to "Hitos"
                ).forEach { (entry, label) ->
                    val selected = currentDestination?.route == entry.first
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (!selected) {
                                    nav.navigate(entry.first) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selected) InstitutionalColors.GreenSoft else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = entry.second,
                                contentDescription = label,
                                tint = if (selected) InstitutionalColors.GreenDark else InstitutionalColors.MutedText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) InstitutionalColors.GreenDark else InstitutionalColors.MutedText,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Dashboard(vm: TutorViewModel, nav: NavHostController, onOpenProfile: () -> Unit) {
    val context = LocalContext.current
    val deliverables by vm.deliverables.collectAsState()
    val checklist by vm.checklist.collectAsState()
    val profile by vm.userProfile.collectAsState()
    val projectTopics by vm.projectTopics.collectAsState()
    val radioState by vm.radioState.collectAsState()
    val summary = calculateProgress(deliverables, vm.stages, checklist)
    val progress by animateFloatAsState(summary.fraction, label = "progress")

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        vm.toggleRadio()
    }

    fun toggleRadioWithNotificationPermission() {
        val needsStart = radioState == RadioPlaybackState.STOPPED || radioState == RadioPlaybackState.ERROR
        if (needsStart &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.toggleRadio()
        }
    }

    var showAddTopicDialog by remember { mutableStateOf(false) }
    var deletingTopic by remember { mutableStateOf<ProjectTopic?>(null) }
    var newTopicText by remember { mutableStateOf("") }
    var newTopicSubmitted by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    Text("Progreso general del proyecto", fontWeight = FontWeight.SemiBold, color = InstitutionalColors.GreenDark, fontSize = 14.sp)
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
                        modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(5.dp)),
                        color = InstitutionalColors.Green,
                        trackColor = InstitutionalColors.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${summary.approvedDeliverables} de ${deliverables.size} hitos aprobados · " +
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
            val isRadioActive = radioState == RadioPlaybackState.PLAYING || radioState == RadioPlaybackState.BUFFERING
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(
                    label = when (radioState) {
                        RadioPlaybackState.PLAYING -> "Radio\nEn vivo"
                        RadioPlaybackState.PAUSED -> "Radio\nEn pausa"
                        RadioPlaybackState.BUFFERING -> "Radio\nConectando"
                        else -> "Radio\nCarabineros"
                    },
                    icon = Icons.Default.Radio,
                    highlighted = isRadioActive
                ) { toggleRadioWithNotificationPermission() }

                QuickAction(
                    label = "Revista\nCarabineros",
                    icon = Icons.Default.Newspaper
                ) {
                    openExternalUrl(
                        context = context,
                        url = "https://www.revistacarabineros.cl/",
                        vm = vm,
                        loadingText = "Abriendo Revista Carabineros..."
                    )
                }

                QuickAction(
                    label = "Museo de\nCarabineros",
                    icon = Icons.Default.Museum
                ) {
                    openExternalUrl(
                        context = context,
                        url = "https://www.museocarabineros.cl/web/sitio",
                        vm = vm,
                        loadingText = "Abriendo Museo de Carabineros..."
                    )
                }
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
                    onClick = {
                        newTopicText = ""
                        newTopicSubmitted = false
                        showAddTopicDialog = true
                    },
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

        if (projectTopics.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                    border = BorderStroke(1.dp, InstitutionalColors.Border),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
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
            items(projectTopics, key = { it.id }) { registeredTopic ->
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
                            onClick = { deletingTopic = registeredTopic },
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
                    modifier = Modifier.fillMaxWidth().clickable { onOpenProfile() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SupervisedUserCircle, null, tint = InstitutionalColors.GreenDark, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            if (profile.tutorName.isNotBlank()) {
                                Text("Tutor asignado: ${profile.tutorName}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = InstitutionalColors.Graphite)
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

        item { SectionTitle("Próximo paso", "Ver guía metodológica") { nav.navigate("guide") } }

        item {
            deliverables.firstOrNull { it.status != DeliverableStatus.APPROVED }?.let { next ->
                DeliverableCard(next, vm)
            }
        }
    }

    if (showAddTopicDialog) {
        val validNewTopic = newTopicText.trim().length >= 4
        AlertDialog(
            onDismissRequest = { showAddTopicDialog = false },
            title = { Text("Añadir tema de proyecto", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Una vez registrado, este tema queda guardado de forma permanente y no podrá modificarse (solo eliminarse).",
                        fontSize = 12.sp,
                        color = InstitutionalColors.MutedText
                    )
                    OutlinedTextField(
                        value = newTopicText,
                        onValueChange = { newTopicText = it },
                        label = { Text("Título del tema de investigación") },
                        maxLines = 3,
                        isError = newTopicSubmitted && !validNewTopic,
                        supportingText = {
                            if (newTopicSubmitted && !validNewTopic) Text("Ingresa al menos 4 caracteres")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        newTopicSubmitted = true
                        if (validNewTopic) {
                            vm.addProjectTopic(newTopicText)
                            showAddTopicDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InstitutionalColors.Green)
                ) {
                    Text("Registrar tema")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTopicDialog = false }) { Text("Cancelar") }
            }
        )
    }

    deletingTopic?.let { topicToDelete ->
        AlertDialog(
            onDismissRequest = { deletingTopic = null },
            title = { Text("Eliminar tema registrado") },
            text = { Text("¿Deseas eliminar '${topicToDelete.title}' del registro de temas?") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteProjectTopic(topicToDelete.id)
                        deletingTopic = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { deletingTopic = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun NewsScreen(vm: TutorViewModel) {
    val context = LocalContext.current
    val officialUrl = "https://www.revistacarabineros.cl/"

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Noticias institucionales", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = InstitutionalColors.GreenDark)
            Spacer(Modifier.height(2.dp))
            Text(
                "Accede a las publicaciones oficiales de la Revista Carabineros.",
                color = InstitutionalColors.MutedText,
                fontSize = 14.sp
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = InstitutionalColors.NavySoft),
                border = BorderStroke(1.dp, InstitutionalColors.Navy.copy(alpha = 0.18f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = InstitutionalColors.Navy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Newspaper,
                                contentDescription = null,
                                tint = InstitutionalColors.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Text(
                        "Revista Carabineros",
                        color = InstitutionalColors.Navy,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Consulta noticias, reportajes y contenidos institucionales directamente desde el portal oficial.",
                        color = InstitutionalColors.Graphite,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Button(
                        onClick = {
                            openExternalUrl(
                                context = context,
                                url = officialUrl,
                                vm = vm,
                                loadingText = "Abriendo Revista Carabineros..."
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InstitutionalColors.Navy,
                            contentColor = InstitutionalColors.White
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Abrir Revista Carabineros", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                border = BorderStroke(1.dp, InstitutionalColors.Border),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, null, tint = InstitutionalColors.Green)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Fuente oficial", fontWeight = FontWeight.SemiBold, color = InstitutionalColors.Graphite)
                        Text(
                            "Los contenidos se consultan en el sitio institucional para conservar la información actualizada.",
                            color = InstitutionalColors.MutedText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String?, onClick: () -> Unit) {
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
private fun RowScope.QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.weight(1f).clickable { onClick() },
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
            Modifier.padding(vertical = 12.dp, horizontal = 6.dp).fillMaxWidth(),
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
private fun Guide(vm: TutorViewModel, nav: NavHostController) {
    val checklist by vm.checklist.collectAsState()

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Guía metodológica", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Cinco etapas estructuradas para tu Trabajo de Título.")
        }

        items(vm.stages) { stage ->
            val completedCount = checklist[stage.id].orEmpty().count { it in stage.checklist.indices }
            val totalCount = stage.checklist.size
            val stageProgress = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount

            Card(
                modifier = Modifier.fillMaxWidth().clickable { nav.navigate("stage/${stage.id}") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                border = BorderStroke(1.dp, InstitutionalColors.Border)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).background(Color(stage.color), RoundedCornerShape(6.dp)))
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
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(stage.color),
                        trackColor = Color(stage.color).copy(alpha = 0.15f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StageDetail(vm: TutorViewModel, stage: Stage, nav: NavHostController) {
    val context = LocalContext.current
    val checklist by vm.checklist.collectAsState()
    val stageNotes by vm.stageNotes.collectAsState()

    var noteText by remember(stage.id) { mutableStateOf(stageNotes[stage.id] ?: "") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var documentName by remember { mutableStateOf<String?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap = it }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { documentName = it?.lastPathSegment }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) camera.launch(null)
        else Toast.makeText(context, "Se necesita permiso de cámara para registrar evidencia", Toast.LENGTH_SHORT).show()
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
            val done = checklist[stage.id].orEmpty().contains(index)
            Row(
                Modifier.fillMaxWidth().clickable { vm.setChecklistItem(stage.id, index, !done) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = done, onCheckedChange = { vm.setChecklistItem(stage.id, index, it) })
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
                    Text("Escribe aquí tus dudas para tu Profesor Tutor o ideas de tu avance. Se guarda automáticamente.", fontSize = 12.sp, color = InstitutionalColors.Graphite)
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = {
                            noteText = it
                            vm.saveStageNote(stage.id, it)
                        },
                        placeholder = { Text("Ej. Consultar con tutor sobre la matriz de datos...") },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
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
                                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) camera.launch(null)
                                else cameraPermission.launch(Manifest.permission.CAMERA)
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
                            Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    documentName?.let { Text("Documento seleccionado: $it", color = InstitutionalColors.Navy, fontSize = 13.sp) }
                }
            }
        }

        item {
            TextButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Volver a la guía")
            }
        }
    }
}

@Composable
private fun StudyToolsScreen(vm: TutorViewModel) {
    val context = LocalContext.current
    val seconds by vm.pomodoroSeconds.collectAsState()
    val isRunning by vm.isPomodoroRunning.collectAsState()
    val mode by vm.pomodoroMode.collectAsState()
    val radioState by vm.radioState.collectAsState()
    val radioError by vm.radioError.collectAsState()

    var apaType by remember { mutableStateOf("Ley") }
    var apaAuthor by remember { mutableStateOf("") }
    var apaYear by remember { mutableStateOf("") }
    var apaTitle by remember { mutableStateOf("") }
    var apaSource by remember { mutableStateOf("") }

    val generatedCitation = remember(apaType, apaAuthor, apaYear, apaTitle, apaSource) {
        generateApaCitation(apaType, apaAuthor, apaYear, apaTitle, apaSource)
    }

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
                    Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = InstitutionalColors.White.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            if (mode == "study") "MÉTODO POMODORO · BLOQUE DE ENFOQUE" else "MÉTODO POMODORO · DESCANSO",
                            color = InstitutionalColors.GreenDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))

                    val minutesStr = "%02d".format(seconds / 60)
                    val secondsStr = "%02d".format(seconds % 60)

                    Text(
                        "$minutesStr:$secondsStr",
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        color = InstitutionalColors.GreenDark
                    )

                    Text(
                        if (mode == "study") "25 min de lectura y redacción sin distracciones" else "5 min para pausar y descansar la vista",
                        fontSize = 13.sp,
                        color = InstitutionalColors.MutedText
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!isRunning) {
                            Button(
                                onClick = { vm.startPomodoro() },
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
                                onClick = { vm.pausePomodoro() },
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
                            onClick = { vm.resetPomodoro() },
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
                        listOf("Ley", "Reglamento", "Libro").forEach { t ->
                            FilterChip(
                                selected = apaType == t,
                                onClick = { apaType = t },
                                label = { Text(t) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = apaAuthor,
                        onValueChange = { apaAuthor = it },
                        singleLine = true,
                        label = { Text(if (apaType == "Ley") "Ministerio / Emisor (Ej. Min. Defensa)" else "Autor / Institución") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = apaYear,
                            onValueChange = { apaYear = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text("Año") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = apaTitle,
                            onValueChange = { apaTitle = it },
                            singleLine = true,
                            label = { Text(if (apaType == "Ley") "N° Ley / Título" else "Título") },
                            modifier = Modifier.weight(2f)
                        )
                    }

                    OutlinedTextField(
                        value = apaSource,
                        onValueChange = { apaSource = it },
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
                            Text("Cita Generada en Formato APA 7:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InstitutionalColors.GreenDark)
                            Spacer(Modifier.height(4.dp))
                            Text(generatedCitation, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Cita APA 7", generatedCitation)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Cita copiada al portapapeles", Toast.LENGTH_SHORT).show()
                                },
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
                    Text("Asegúrate de que los 5 elementos de tu Trabajo de Título estén alineados:", fontSize = 13.sp, color = InstitutionalColors.Graphite)

                    listOf(
                        "1. Problema" to "Situación operacional/policial que requiere diagnóstico o propuesta de solución.",
                        "2. Pregunta" to "¿Cómo/Qué/De qué manera influye el problema en la función institucional?",
                        "3. Obj. General" to "Verbo en infinitivo + responde directamente a la pregunta de investigación.",
                        "4. Obj. Específicos" to "Pasos ordenados para cumplir el objetivo general (ej. Diagnosticar, Analizar, Proponer).",
                        "5. Metodología" to "Enfoque (cuantitativo/cualitativo), población/muestra e instrumentos de recolección."
                    ).forEach { (title, desc) ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = InstitutionalColors.GreenDark, modifier = Modifier.width(108.dp))
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
                            .clickable {
                                openExternalUrl(
                                    context = context,
                                    url = "https://www.museocarabineros.cl/web/sitio",
                                    vm = vm,
                                    loadingText = "Abriendo Museo Histórico..."
                                )
                            }
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
                            .clickable { vm.toggleRadio() }
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
                                    Surface(
                                        color = InstitutionalColors.White,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "FM 98.1",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = InstitutionalColors.Navy,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        color = InstitutionalColors.White,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
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

@Composable
private fun FaqScreen(vm: TutorViewModel) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Todas") }
    val categories = listOf("Todas", "Metodología", "Citación APA", "Formato", "Estudio")

    val filtered = vm.faqs.filter { faq ->
        (category == "Todas" || faq.category == category) &&
            (query.isBlank() || faq.question.contains(query, true) || faq.answer.contains(query, true))
    }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Preguntas frecuentes", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar por palabra clave") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                    border = BorderStroke(1.dp, InstitutionalColors.Border)
                ) {
                    Column(
                        Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.SearchOff, null, tint = InstitutionalColors.Graphite, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Sin resultados", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("No encontramos respuestas para '$query'. Intenta buscar otro término.", fontSize = 13.sp, color = InstitutionalColors.Graphite)
                    }
                }
            }
        } else {
            items(filtered) { faq ->
                var expanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                    border = BorderStroke(1.dp, InstitutionalColors.Border)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(faq.category.uppercase(), color = InstitutionalColors.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = InstitutionalColors.Graphite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(faq.question, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = InstitutionalColors.Graphite)
                        AnimatedVisibility(expanded) {
                            Text(
                                faq.answer,
                                Modifier.padding(top = 8.dp),
                                color = InstitutionalColors.MutedText,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Deliverables(vm: TutorViewModel) {
    val items by vm.deliverables.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingDeliverable by remember { mutableStateOf<Deliverable?>(null) }
    var deletingDeliverable by remember { mutableStateOf<Deliverable?>(null) }

    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showDeadlineNotification(context)
    }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Mis hitos académicos", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = InstitutionalColors.Green,
                    contentColor = InstitutionalColors.White,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Add, "Agregar Hito")
                }
            }
        }

        items(items) { item ->
            DeliverableCardWithActions(
                item = item,
                vm = vm,
                onEdit = { editingDeliverable = item },
                onDelete = { deletingDeliverable = item }
            )
        }

        item {
            OutlinedButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        showDeadlineNotification(context)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Notifications, null)
                Spacer(Modifier.width(8.dp))
                Text("Activar recordatorios periódicos")
            }
        }
    }

    if (showAddDialog) {
        AddEditDeliverableDialog(
            item = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, date ->
                vm.addDeliverable(title, date)
                showAddDialog = false
            }
        )
    }

    editingDeliverable?.let { item ->
        AddEditDeliverableDialog(
            item = item,
            onDismiss = { editingDeliverable = null },
            onConfirm = { title, date ->
                vm.updateDeliverable(item.id, title, date, item.status)
                editingDeliverable = null
            }
        )
    }

    deletingDeliverable?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingDeliverable = null },
            title = { Text("Eliminar Hito") },
            text = { Text("¿Deseas eliminar el hito '${item.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteDeliverable(item.id)
                        deletingDeliverable = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { deletingDeliverable = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun DeliverableCardWithActions(
    item: Deliverable,
    vm: TutorViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
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
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).width(156.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DeliverableStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.label) },
                                onClick = {
                                    vm.updateStatus(item.id, status)
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
private fun DeliverableCard(item: Deliverable, vm: TutorViewModel) {
    DeliverableCardWithActions(item = item, vm = vm, onEdit = {}, onDelete = {}, showActions = false)
}

@Composable
private fun UrgencyBadge(urgency: DeliverableUrgency) {
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
private fun StatusBadge(status: DeliverableStatus) {
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

@Composable
private fun AddEditDeliverableDialog(
    item: Deliverable?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, dueDate: String) -> Unit
) {
    var title by remember { mutableStateOf(item?.title ?: "") }
    var date by remember { mutableStateOf(item?.dueDate ?: "") }
    var submitted by remember { mutableStateOf(false) }

    val validTitle = title.trim().length >= 3
    val validDate = date.trim().length >= 4

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Nuevo Hito Académico" else "Editar Hito") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre del Hito") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = submitted && !validTitle,
                    supportingText = { if (submitted && !validTitle) Text("Ingresa al menos 3 caracteres") }
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Fecha límite (ej. 20 dic 2026)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = submitted && !validDate,
                    supportingText = { if (submitted && !validDate) Text("Indica la fecha de entrega") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    submitted = true
                    if (validTitle && validDate) {
                        onConfirm(title.trim(), date.trim())
                    }
                },
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

private fun showDeadlineNotification(context: Context) {
    if (Build.VERSION.SDK_INT >= 33 &&
        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        Toast.makeText(context, "Concede permiso de notificaciones para recibir recordatorios", Toast.LENGTH_SHORT).show()
        return
    }

    val notification = NotificationCompat.Builder(context, "deadlines")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Tutor Virtual ESUCAR")
        .setContentText("Recuerda revisar tus hitos académicos para tu Trabajo de Título.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(1001, notification)
}
