@file:OptIn(ExperimentalMaterial3Api::class)

package cl.esucar.tutorvirtual

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import cl.esucar.tutorvirtual.data.worker.DeadlineReminderWorker
import cl.esucar.tutorvirtual.domain.model.InstitutionalLinks
import cl.esucar.tutorvirtual.ui.components.AddEditDeliverableDialog
import cl.esucar.tutorvirtual.ui.components.AddTopicDialog
import cl.esucar.tutorvirtual.ui.components.BottomNavBar
import cl.esucar.tutorvirtual.ui.components.DeleteDeliverableDialog
import cl.esucar.tutorvirtual.ui.components.DeleteTopicDialog
import cl.esucar.tutorvirtual.ui.components.GlobalLoadingOverlay
import cl.esucar.tutorvirtual.ui.components.InstitutionalBrand
import cl.esucar.tutorvirtual.ui.components.MiniRadioBar
import cl.esucar.tutorvirtual.ui.components.ProfileDialog
import cl.esucar.tutorvirtual.ui.screens.DashboardScreen
import cl.esucar.tutorvirtual.ui.screens.DeliverablesScreen
import cl.esucar.tutorvirtual.ui.screens.FaqScreen
import cl.esucar.tutorvirtual.ui.screens.GuideScreen
import cl.esucar.tutorvirtual.ui.screens.LoginScreen
import cl.esucar.tutorvirtual.ui.screens.NewsScreen
import cl.esucar.tutorvirtual.ui.screens.StageDetailScreen
import cl.esucar.tutorvirtual.ui.screens.StudyToolsScreen
import cl.esucar.tutorvirtual.ui.state.MainUiEvent
import cl.esucar.tutorvirtual.ui.theme.InstitutionalColors
import cl.esucar.tutorvirtual.ui.theme.InstitutionalLightColors
import cl.esucar.tutorvirtual.ui.viewmodel.AuthViewModel
import cl.esucar.tutorvirtual.ui.viewmodel.TutorMainViewModel
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { (application as TutorApplication).container }

    private val authViewModel: AuthViewModel by viewModels {
        appContainer.authViewModelFactory()
    }

    private val mainViewModel: TutorMainViewModel by viewModels {
        appContainer.mainViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        scheduleDeadlineReminders()
        setContent {
            TutorAppRoot(
                authViewModel = authViewModel,
                mainViewModel = mainViewModel
            )
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "deadlines",
            "Recordatorios académicos",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun scheduleDeadlineReminders() {
        val request = PeriodicWorkRequestBuilder<DeadlineReminderWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "deadline-reminders",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

@Composable
fun TutorAppRoot(
    authViewModel: AuthViewModel,
    mainViewModel: TutorMainViewModel
) {
    val context = LocalContext.current
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()

    val radioNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        mainViewModel.toggleRadio()
    }

    val deadlineNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showDeadlineNotification(context)
        else Toast.makeText(context, "Concede permiso de notificaciones para recibir recordatorios", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(mainViewModel, authViewModel) {
        mainViewModel.events.collect { event ->
            when (event) {
                is MainUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is MainUiEvent.OpenExternalUrl -> {
                    openBrowserUrl(context, event.url)
                }
                is MainUiEvent.CopyToClipboard -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(event.label, event.text))
                    Toast.makeText(context, "Cita copiada al portapapeles", Toast.LENGTH_SHORT).show()
                }
                MainUiEvent.RequestRadioWithNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        radioNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        mainViewModel.toggleRadio()
                    }
                }
                MainUiEvent.TriggerDeadlineNotification -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        deadlineNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        showDeadlineNotification(context)
                    }
                }
                MainUiEvent.LoggedOut -> {
                    authViewModel.clearSensitiveFieldsAfterLogout()
                }
            }
        }
    }

    MaterialTheme(colorScheme = InstitutionalLightColors) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!mainState.userProfile.isLoggedIn) {
                LoginScreen(
                    state = authState,
                    onModeChanged = authViewModel::onModeChanged,
                    onUserRoleSelected = authViewModel::onUserRoleSelected,
                    onRankSelected = authViewModel::onRankSelected,
                    onTutorSelected = authViewModel::onTutorSelected,
                    onFirstNameChanged = authViewModel::onFirstNameChanged,
                    onFirstLastNameChanged = authViewModel::onFirstLastNameChanged,
                    onSecondLastNameChanged = authViewModel::onSecondLastNameChanged,
                    onEmailChanged = authViewModel::onEmailChanged,
                    onRegenerateEmail = authViewModel::onRegenerateEmail,
                    onApplyDomainSuggestion = authViewModel::onApplyDomainSuggestion,
                    onPasswordChanged = authViewModel::onPasswordChanged,
                    onConfirmPasswordChanged = authViewModel::onConfirmPasswordChanged,
                    onTogglePasswordVisibility = authViewModel::onTogglePasswordVisibility,
                    onToggleConfirmPasswordVisibility = authViewModel::onToggleConfirmPasswordVisibility,
                    onTopicChanged = authViewModel::onTopicChanged,
                    onSubmit = authViewModel::onSubmit,
                    onQuickDemoLogin = { authViewModel.onQuickDemoLogin() }
                )
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
                                IconButton(onClick = mainViewModel::openProfileEditor) {
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
                            MiniRadioBar(
                                radioState = mainState.radioState,
                                radioError = mainState.radioError,
                                radioVolume = mainState.radioVolume,
                                onPlay = mainViewModel::playRadio,
                                onPause = mainViewModel::pauseRadio,
                                onResume = mainViewModel::resumeRadio,
                                onStop = mainViewModel::stopRadio,
                                onVolumeChange = mainViewModel::setRadioVolume,
                                onToggleMute = mainViewModel::toggleRadioMute
                            )
                            BottomNavBar(
                                currentRoute = currentDestination?.route,
                                onNavigate = { route ->
                                    nav.navigate(route) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { padding ->
                    NavHost(nav, startDestination = "dashboard", Modifier.padding(padding)) {
                        composable("dashboard") {
                            DashboardScreen(
                                state = mainState,
                                onRadioClick = mainViewModel::onRadioQuickActionClicked,
                                onRevistaClick = {
                                    mainViewModel.openExternalUrl(
                                        InstitutionalLinks.REVISTA_CARABINEROS,
                                        "Abriendo Revista Carabineros..."
                                    )
                                },
                                onMuseoClick = {
                                    mainViewModel.openExternalUrl(
                                        InstitutionalLinks.MUSEO_CARABINEROS,
                                        "Abriendo Museo de Carabineros..."
                                    )
                                },
                                onAddTopicClick = mainViewModel::openAddTopicDialog,
                                onDeleteTopicClick = mainViewModel::requestDeleteTopic,
                                onOpenProfileClick = mainViewModel::openProfileEditor,
                                onNavigateToGuide = { nav.navigate("guide") },
                                onDeliverableStatusChange = mainViewModel::updateDeliverableStatus
                            )
                        }
                        composable("guide") {
                            GuideScreen(
                                stages = mainState.stages,
                                checklist = mainState.checklist,
                                onStageClick = { stageId -> nav.navigate("stage/$stageId") }
                            )
                        }
                        composable("stage/{id}") { backStack ->
                            val stageId = backStack.arguments?.getString("id")
                            val stage = mainState.stages.firstOrNull { it.id == stageId }
                                ?: mainState.stages.first()
                            StageDetailScreen(
                                stage = stage,
                                completedIndices = mainState.checklist[stage.id].orEmpty(),
                                note = mainState.stageNotes[stage.id].orEmpty(),
                                onChecklistToggle = { index, completed ->
                                    mainViewModel.setChecklistItem(stage.id, index, completed)
                                },
                                onNoteChanged = { note ->
                                    mainViewModel.saveStageNote(stage.id, note)
                                },
                                onPermissionDeniedMessage = mainViewModel::showToast,
                                onBack = { nav.popBackStack() }
                            )
                        }
                        composable("study") {
                            StudyToolsScreen(
                                pomodoro = mainState.pomodoro,
                                apaGenerator = mainState.apaGenerator,
                                radioState = mainState.radioState,
                                radioError = mainState.radioError,
                                onStartPomodoro = mainViewModel::startPomodoro,
                                onPausePomodoro = mainViewModel::pausePomodoro,
                                onResetPomodoro = mainViewModel::resetPomodoro,
                                onApaTypeSelected = mainViewModel::onApaTypeSelected,
                                onApaAuthorChanged = mainViewModel::onApaAuthorChanged,
                                onApaYearChanged = mainViewModel::onApaYearChanged,
                                onApaTitleChanged = mainViewModel::onApaTitleChanged,
                                onApaSourceChanged = mainViewModel::onApaSourceChanged,
                                onCopyCitation = mainViewModel::copyGeneratedApaCitation,
                                onOpenMuseo = {
                                    mainViewModel.openExternalUrl(
                                        InstitutionalLinks.MUSEO_CARABINEROS,
                                        "Abriendo Museo Histórico..."
                                    )
                                },
                                onToggleRadio = mainViewModel::toggleRadio
                            )
                        }
                        composable("faq") {
                            FaqScreen(
                                state = mainState.faq,
                                onQueryChanged = mainViewModel::onFaqQueryChanged,
                                onCategorySelected = mainViewModel::onFaqCategorySelected
                            )
                        }
                        composable("deliverables") {
                            DeliverablesScreen(
                                deliverables = mainState.deliverables,
                                onAddClick = mainViewModel::openAddDeliverableDialog,
                                onEditClick = mainViewModel::openEditDeliverableDialog,
                                onDeleteClick = mainViewModel::requestDeleteDeliverable,
                                onStatusSelected = mainViewModel::updateDeliverableStatus,
                                onActivateRemindersClick = mainViewModel::onTriggerDeadlineNotificationClicked
                            )
                        }
                        composable("news") {
                            NewsScreen(
                                onOpenRevista = {
                                    mainViewModel.openExternalUrl(
                                        InstitutionalLinks.REVISTA_CARABINEROS,
                                        "Abriendo Revista Carabineros..."
                                    )
                                }
                            )
                        }
                    }
                }

                ProfileDialog(
                    state = mainState.profileEditor,
                    onRoleSelected = mainViewModel::onProfileRoleSelected,
                    onRankSelected = mainViewModel::onProfileRankSelected,
                    onFirstNameChanged = mainViewModel::onProfileFirstNameChanged,
                    onFirstLastNameChanged = mainViewModel::onProfileFirstLastNameChanged,
                    onSecondLastNameChanged = mainViewModel::onProfileSecondLastNameChanged,
                    onEmailChanged = mainViewModel::onProfileEmailChanged,
                    onTutorSelected = mainViewModel::onProfileTutorSelected,
                    onSave = mainViewModel::saveProfileChanges,
                    onLogout = mainViewModel::logoutUser,
                    onDismiss = mainViewModel::dismissProfileEditor
                )

                AddTopicDialog(
                    state = mainState.topicRegistry,
                    onTopicTextChanged = mainViewModel::onNewTopicTextChanged,
                    onConfirm = mainViewModel::confirmAddTopic,
                    onDismiss = mainViewModel::dismissAddTopicDialog
                )

                DeleteTopicDialog(
                    topic = mainState.topicRegistry.deletingTopic,
                    onConfirm = mainViewModel::confirmDeleteTopic,
                    onDismiss = mainViewModel::dismissDeleteTopic
                )

                AddEditDeliverableDialog(
                    state = mainState.deliverableEditor,
                    onTitleChanged = mainViewModel::onDeliverableTitleChanged,
                    onDueDateChanged = mainViewModel::onDeliverableDueDateChanged,
                    onConfirm = mainViewModel::confirmSaveDeliverable,
                    onDismiss = mainViewModel::dismissDeliverableDialog
                )

                DeleteDeliverableDialog(
                    item = mainState.deliverableEditor.deletingItem,
                    onConfirm = mainViewModel::confirmDeleteDeliverable,
                    onDismiss = mainViewModel::dismissDeleteDeliverable
                )
            }

            val globalLoadingVisible = if (!mainState.userProfile.isLoggedIn) {
                authState.isLoading
            } else {
                mainState.isLoading
            }
            val globalLoadingMessage = if (!mainState.userProfile.isLoggedIn) {
                authState.loadingMessage
            } else {
                mainState.loadingMessage
            }
            GlobalLoadingOverlay(
                visible = globalLoadingVisible,
                message = globalLoadingMessage
            )
        }
    }
}

private fun openBrowserUrl(context: Context, url: String) {
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
