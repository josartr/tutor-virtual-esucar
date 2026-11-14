package cl.esucar.tutorvirtual.ui

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import cl.esucar.tutorvirtual.data.*
import cl.esucar.tutorvirtual.radio.RadioPlaybackService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RadioPlaybackState {
    STOPPED,
    BUFFERING,
    PLAYING,
    PAUSED,
    ERROR
}

class TutorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalRepository(application)

    private val _userProfile = MutableStateFlow(repository.loadUserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _deliverables = MutableStateFlow(repository.loadDeliverables())
    val deliverables: StateFlow<List<Deliverable>> = _deliverables.asStateFlow()

    private val _checklist = MutableStateFlow(repository.loadChecklist())
    val checklist: StateFlow<Map<String, Set<Int>>> = _checklist.asStateFlow()

    private val _stageNotes = MutableStateFlow(repository.loadStageNotes())
    val stageNotes: StateFlow<Map<String, String>> = _stageNotes.asStateFlow()

    private val _projectTopics = MutableStateFlow(loadOrSeedProjectTopics())
    val projectTopics: StateFlow<List<ProjectTopic>> = _projectTopics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingMessage = MutableStateFlow("Cargando...")
    val loadingMessage: StateFlow<String> = _loadingMessage.asStateFlow()

    val stages = demoStages
    val faqs = demoFaqs
    val apaTemplates = demoApaTemplates

    // Pomodoro study timer state
    private val _pomodoroSeconds = MutableStateFlow(25 * 60)
    val pomodoroSeconds: StateFlow<Int> = _pomodoroSeconds.asStateFlow()

    private val _isPomodoroRunning = MutableStateFlow(false)
    val isPomodoroRunning: StateFlow<Boolean> = _isPomodoroRunning.asStateFlow()

    private val _pomodoroMode = MutableStateFlow("study") // "study" or "rest"
    val pomodoroMode: StateFlow<String> = _pomodoroMode.asStateFlow()

    private var pomodoroJob: Job? = null

    private val _radioState = MutableStateFlow(RadioPlaybackState.STOPPED)
    val radioState: StateFlow<RadioPlaybackState> = _radioState.asStateFlow()

    private val _radioError = MutableStateFlow<String?>(null)
    val radioError: StateFlow<String?> = _radioError.asStateFlow()

    private val _radioVolume = MutableStateFlow(0.85f)
    val radioVolume: StateFlow<Float> = _radioVolume.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var pendingPlayOnConnect = false
    private var radioConnectTimeoutJob: Job? = null

    init {
        connectRadioController()
    }

    private fun loadOrSeedProjectTopics(): List<ProjectTopic> {
        val current = repository.loadProjectTopics()
        val initialTopic = _userProfile.value.researchTopic.trim()
        if (current.isEmpty() && initialTopic.length >= 4) {
            val seeded = ProjectTopic(
                id = "topic-initial-${System.currentTimeMillis()}",
                title = initialTopic,
                createdAt = formatTopicTimestamp()
            )
            repository.addProjectTopic(seeded)
            return listOf(seeded)
        }
        return current
    }

    private fun formatTopicTimestamp(): String =
        SimpleDateFormat("dd MMM yyyy · HH:mm", Locale("es", "ES")).format(Date())

    fun setGlobalLoading(loading: Boolean, message: String = "Cargando...") {
        _loadingMessage.value = message
        _isLoading.value = loading
    }

    fun runWithGlobalLoading(
        message: String,
        minDurationMs: Long = 350L,
        action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            _loadingMessage.value = message
            _isLoading.value = true
            try {
                delay(minDurationMs)
                action()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerUserAsync(
        rank: String,
        firstName: String,
        firstLastName: String,
        secondLastName: String,
        email: String,
        researchTopic: String,
        tutorName: String,
        institutionType: InstitutionType,
        userRole: UserRole = UserRole.ESTUDIANTE,
        password: String,
        onResult: (AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            _loadingMessage.value = "Creando cuenta institucional..."
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) {
                delay(250L)
                registerUser(
                    rank = rank,
                    firstName = firstName,
                    firstLastName = firstLastName,
                    secondLastName = secondLastName,
                    email = email,
                    researchTopic = researchTopic,
                    tutorName = tutorName,
                    institutionType = institutionType,
                    userRole = userRole,
                    password = password
                )
            }
            _isLoading.value = false
            onResult(result)
        }
    }

    fun loginAsync(
        email: String,
        password: String,
        onResult: (AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            _loadingMessage.value = "Verificando credenciales..."
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) {
                delay(250L)
                login(email, password)
            }
            _isLoading.value = false
            onResult(result)
        }
    }

    fun loginQuickSSOAsync(provider: String = "SSO Institucional (Demo)") {
        viewModelScope.launch {
            _loadingMessage.value = "Preparando acceso institucional..."
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                delay(280L)
                loginQuickSSO(provider)
            }
            _isLoading.value = false
        }
    }

    fun updateUserProfileAsync(
        rank: String,
        firstName: String,
        firstLastName: String,
        secondLastName: String,
        email: String,
        tutorName: String,
        userRole: UserRole = _userProfile.value.userRole,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _loadingMessage.value = "Guardando cambios del perfil..."
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                delay(200L)
                updateUserProfile(
                    rank = rank,
                    firstName = firstName,
                    firstLastName = firstLastName,
                    secondLastName = secondLastName,
                    email = email,
                    researchTopic = _userProfile.value.researchTopic,
                    tutorName = tutorName,
                    userRole = userRole
                )
            }
            _isLoading.value = false
            onDone()
        }
    }

    fun registerUser(
        rank: String,
        firstName: String,
        firstLastName: String,
        secondLastName: String,
        email: String,
        researchTopic: String,
        tutorName: String,
        institutionType: InstitutionType,
        userRole: UserRole = UserRole.ESTUDIANTE,
        password: String
    ): AuthResult {
        val cleanFirst = firstName.trim()
        val cleanFirstLast = firstLastName.trim()
        val cleanSecondLast = secondLastName.trim()
        val resolvedInstitution = institutionForEmail(email) ?: institutionType
        val resolvedRank = resolveProfileRank(resolvedInstitution, userRole, rank)
        val profile = UserProfile(
            rank = resolvedRank,
            firstName = cleanFirst,
            firstLastName = cleanFirstLast,
            secondLastName = cleanSecondLast,
            fullName = buildFullName(cleanFirst, cleanFirstLast, cleanSecondLast),
            email = email.trim(),
            researchTopic = researchTopic.trim(),
            tutorName = tutorName.trim(),
            ssoProvider = "Registro institucional",
            institutionType = resolvedInstitution,
            userRole = userRole,
            isLoggedIn = true
        )
        return when (val result = runCatching { repository.registerUser(profile, password) }
            .getOrElse { AuthResult.PersistenceError }) {
            is AuthResult.Success -> {
                _userProfile.value = result.profile
                seedInitialTopicIfNeeded(result.profile.researchTopic)
                result
            }
            else -> result
        }
    }

    fun registerUser(
        rank: String,
        fullName: String,
        email: String,
        researchTopic: String,
        tutorName: String,
        institutionType: InstitutionType,
        password: String
    ): AuthResult {
        val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val first = parts.firstOrNull().orEmpty()
        val last1 = parts.getOrNull(1).orEmpty()
        val last2 = parts.drop(2).joinToString(" ")
        val resolvedInstitution = institutionForEmail(email) ?: institutionType
        val profile = UserProfile(
            rank = resolveProfileRank(resolvedInstitution, UserRole.ESTUDIANTE, rank),
            firstName = first,
            firstLastName = last1,
            secondLastName = last2,
            fullName = fullName.trim(),
            email = email.trim(),
            researchTopic = researchTopic.trim(),
            tutorName = tutorName.trim(),
            ssoProvider = "Registro institucional",
            institutionType = resolvedInstitution,
            userRole = UserRole.ESTUDIANTE,
            isLoggedIn = true
        )
        return when (val result = runCatching { repository.registerUser(profile, password) }
            .getOrElse { AuthResult.PersistenceError }) {
            is AuthResult.Success -> {
                _userProfile.value = result.profile
                seedInitialTopicIfNeeded(result.profile.researchTopic)
                result
            }
            else -> result
        }
    }

    fun login(email: String, password: String): AuthResult {
        return when (val result = runCatching { repository.login(email, password) }
            .getOrElse { AuthResult.PersistenceError }) {
            is AuthResult.Success -> {
                _userProfile.value = result.profile
                seedInitialTopicIfNeeded(result.profile.researchTopic)
                result
            }
            else -> result
        }
    }

    @Deprecated("Kept for compatibility with the legacy demo screen.")
    fun loginUser(
        rank: String,
        fullName: String,
        email: String,
        researchTopic: String,
        tutorName: String,
        ssoProvider: String = "Correo Institucional"
    ) {
        val institutionType = if (email.trim().lowercase().endsWith("@carabineros.cl")) {
            InstitutionType.CARABINEROS
        } else {
            InstitutionType.ESUCAR
        }
        registerUser(
            rank = rank,
            fullName = fullName,
            email = email,
            researchTopic = researchTopic,
            tutorName = tutorName,
            institutionType = institutionType,
            password = "LegacyDemo123!"
        )
    }

    fun loginQuickSSO(provider: String = "Carabineros SSO") {
        val defaultProfile = UserProfile(
            rank = "Sargento 2°",
            firstName = "Marcos",
            firstLastName = "Soto",
            secondLastName = "Rojas",
            fullName = "Marcos Soto Rojas",
            email = "marcos.soto@carabineros.cl",
            researchTopic = "Estrategias de Seguridad Preventiva Cuadrante",
            tutorName = "Mayor R. González",
            ssoProvider = provider,
            institutionType = InstitutionType.CARABINEROS,
            userRole = UserRole.FUNCIONARIO,
            isLoggedIn = true
        )
        _userProfile.value = defaultProfile
        repository.saveUserProfile(defaultProfile)
        seedInitialTopicIfNeeded(defaultProfile.researchTopic)
    }

    fun updateUserProfile(
        rank: String,
        firstName: String,
        firstLastName: String,
        secondLastName: String,
        email: String,
        researchTopic: String,
        tutorName: String,
        userRole: UserRole = _userProfile.value.userRole
    ) {
        val cleanFirst = firstName.trim()
        val cleanLast1 = firstLastName.trim()
        val cleanLast2 = secondLastName.trim()
        val resolvedFull = buildFullName(cleanFirst, cleanLast1, cleanLast2)
            .ifBlank { _userProfile.value.fullName }
        val resolvedInstitution = institutionForEmail(email) ?: _userProfile.value.institutionType
        val resolvedRank = resolveProfileRank(resolvedInstitution, userRole, rank)
        val profile = _userProfile.value.copy(
            rank = resolvedRank,
            firstName = cleanFirst,
            firstLastName = cleanLast1,
            secondLastName = cleanLast2,
            fullName = resolvedFull,
            email = email,
            researchTopic = researchTopic,
            tutorName = tutorName,
            institutionType = resolvedInstitution,
            userRole = userRole
        )
        _userProfile.value = profile
        repository.saveUserProfile(profile)
    }

    fun updateUserProfile(
        rank: String,
        fullName: String,
        email: String,
        researchTopic: String,
        tutorName: String
    ) {
        val profile = _userProfile.value.copy(
            rank = rank,
            fullName = fullName,
            email = email,
            researchTopic = researchTopic,
            tutorName = tutorName
        )
        _userProfile.value = profile
        repository.saveUserProfile(profile)
    }

    private fun seedInitialTopicIfNeeded(topicTitle: String) {
        val clean = topicTitle.trim()
        if (clean.length < 4) return
        val existing = repository.loadProjectTopics()
        if (existing.none { it.title.equals(clean, ignoreCase = true) }) {
            val newTopic = ProjectTopic(
                id = "topic-${System.currentTimeMillis()}",
                title = clean,
                createdAt = formatTopicTimestamp()
            )
            repository.addProjectTopic(newTopic)
            _projectTopics.value = repository.loadProjectTopics()
        } else {
            _projectTopics.value = existing
        }
    }

    fun addProjectTopic(title: String) {
        val clean = title.trim()
        if (clean.length < 4) return
        val topic = ProjectTopic(
            id = "topic-${System.currentTimeMillis()}",
            title = clean,
            createdAt = formatTopicTimestamp()
        )
        repository.addProjectTopic(topic)
        val updated = repository.loadProjectTopics()
        _projectTopics.value = updated
        if (_userProfile.value.researchTopic.isBlank()) {
            val updatedProfile = _userProfile.value.copy(researchTopic = clean)
            _userProfile.value = updatedProfile
            repository.saveUserProfile(updatedProfile)
        }
    }

    fun deleteProjectTopic(id: String) {
        repository.deleteProjectTopic(id)
        val updated = repository.loadProjectTopics()
        _projectTopics.value = updated
        val nextTopic = updated.firstOrNull()?.title.orEmpty()
        if (_userProfile.value.researchTopic != nextTopic) {
            val updatedProfile = _userProfile.value.copy(researchTopic = nextTopic)
            _userProfile.value = updatedProfile
            repository.saveUserProfile(updatedProfile)
        }
    }

    fun logoutUser() {
        stopRadio()
        val cleared = UserProfile(isLoggedIn = false)
        _userProfile.value = cleared
        repository.clearUserProfile()
    }

    fun updateStatus(id: String, status: DeliverableStatus) {
        val updated = _deliverables.value.map { if (it.id == id) it.copy(status = status) else it }
        _deliverables.value = updated
        repository.saveDeliverables(updated)
    }

    fun addDeliverable(title: String, dueDate: String, stageId: String = "planteamiento") {
        val item = Deliverable("custom-${System.currentTimeMillis()}", title, dueDate, DeliverableStatus.TODO, stageId)
        val updated = _deliverables.value + item
        _deliverables.value = updated
        repository.saveDeliverables(updated)
    }

    fun updateDeliverable(id: String, title: String, dueDate: String, status: DeliverableStatus) {
        val updated = _deliverables.value.map {
            if (it.id == id) it.copy(title = title, dueDate = dueDate, status = status) else it
        }
        _deliverables.value = updated
        repository.saveDeliverables(updated)
    }

    fun deleteDeliverable(id: String) {
        val updated = _deliverables.value.filterNot { it.id == id }
        _deliverables.value = updated
        repository.saveDeliverables(updated)
    }

    fun setChecklistItem(stageId: String, index: Int, completed: Boolean) {
        val current = _checklist.value.toMutableMap()
        val items = current[stageId].orEmpty().toMutableSet()
        if (completed) items.add(index) else items.remove(index)
        current[stageId] = items
        _checklist.value = current
        repository.saveChecklist(current)
    }

    fun saveStageNote(stageId: String, note: String) {
        val updated = _stageNotes.value.toMutableMap()
        updated[stageId] = note
        _stageNotes.value = updated
        repository.saveStageNote(stageId, note)
    }

    fun startPomodoro() {
        if (_isPomodoroRunning.value) return
        _isPomodoroRunning.value = true
        pomodoroJob = viewModelScope.launch {
            while (_isPomodoroRunning.value && _pomodoroSeconds.value > 0) {
                delay(1000L)
                _pomodoroSeconds.value -= 1
            }
            if (_pomodoroSeconds.value == 0) {
                _isPomodoroRunning.value = false
                togglePomodoroMode()
            }
        }
    }

    fun pausePomodoro() {
        _isPomodoroRunning.value = false
        pomodoroJob?.cancel()
    }

    fun resetPomodoro() {
        pausePomodoro()
        _pomodoroSeconds.value = if (_pomodoroMode.value == "study") 25 * 60 else 5 * 60
    }

    private fun togglePomodoroMode() {
        if (_pomodoroMode.value == "study") {
            _pomodoroMode.value = "rest"
            _pomodoroSeconds.value = 5 * 60
        } else {
            _pomodoroMode.value = "study"
            _pomodoroSeconds.value = 25 * 60
        }
    }

    fun toggleRadio() {
        when (_radioState.value) {
            RadioPlaybackState.PLAYING -> pauseRadio()
            RadioPlaybackState.PAUSED -> resumeRadio()
            RadioPlaybackState.BUFFERING -> stopRadio()
            RadioPlaybackState.STOPPED,
            RadioPlaybackState.ERROR -> playRadio()
        }
    }

    @OptIn(UnstableApi::class)
    fun playRadio() {
        _radioError.value = null
        _radioState.value = RadioPlaybackState.BUFFERING
        _loadingMessage.value = "Conectando con Radio Carabineros..."
        _isLoading.value = true

        radioConnectTimeoutJob?.cancel()
        radioConnectTimeoutJob = viewModelScope.launch {
            delay(3000L)
            if (_isLoading.value && _loadingMessage.value == "Conectando con Radio Carabineros...") {
                _isLoading.value = false
            }
        }

        val controller = mediaController
        if (controller != null) {
            pendingPlayOnConnect = false
            startStreamOnController(controller)
        } else {
            pendingPlayOnConnect = true
            connectRadioController()
        }
    }

    fun pauseRadio() {
        radioConnectTimeoutJob?.cancel()
        if (_loadingMessage.value == "Conectando con Radio Carabineros...") {
            _isLoading.value = false
        }
        pendingPlayOnConnect = false
        mediaController?.let { controller ->
            controller.playWhenReady = false
            controller.pause()
        }
        _radioState.value = RadioPlaybackState.PAUSED
    }

    fun resumeRadio() {
        val controller = mediaController
        if (controller == null ||
            controller.mediaItemCount == 0 ||
            controller.playbackState == Player.STATE_IDLE ||
            controller.playbackState == Player.STATE_ENDED
        ) {
            playRadio()
            return
        }
        _radioError.value = null
        controller.volume = _radioVolume.value
        controller.playWhenReady = true
        controller.play()
        _radioState.value = if (controller.playbackState == Player.STATE_READY) {
            RadioPlaybackState.PLAYING
        } else {
            RadioPlaybackState.BUFFERING
        }
    }

    fun setRadioVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _radioVolume.value = clamped
        mediaController?.volume = clamped
    }

    fun stopRadio() {
        pendingPlayOnConnect = false
        radioConnectTimeoutJob?.cancel()
        if (_loadingMessage.value == "Conectando con Radio Carabineros...") {
            _isLoading.value = false
        }
        mediaController?.let { controller ->
            controller.playWhenReady = false
            controller.stop()
            controller.clearMediaItems()
            controller.sendCustomCommand(
                SessionCommand(RadioPlaybackService.ACTION_STOP_RADIO, Bundle.EMPTY),
                Bundle.EMPTY
            )
        }
        _radioError.value = null
        _radioState.value = RadioPlaybackState.STOPPED
    }

    private fun startStreamOnController(controller: MediaController) {
        controller.volume = _radioVolume.value
        controller.setMediaItem(RadioPlaybackService.buildRadioMediaItem(getApplication()))
        controller.prepare()
        controller.playWhenReady = true
        controller.play()
    }

    private val radioPlayerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            syncRadioStateFromController(mediaController)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncRadioStateFromController(mediaController)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            syncRadioStateFromController(mediaController)
        }

        override fun onVolumeChanged(volume: Float) {
            _radioVolume.value = volume.coerceIn(0f, 1f)
        }

        override fun onPlayerError(error: PlaybackException) {
            radioConnectTimeoutJob?.cancel()
            _isLoading.value = false
            _radioState.value = RadioPlaybackState.ERROR
            _radioError.value = "No se pudo reproducir la señal en vivo. Verifica tu conexión a internet."
        }
    }

    private fun syncRadioStateFromController(controller: MediaController?) {
        if (controller == null) return
        if (controller.playerError != null) {
            radioConnectTimeoutJob?.cancel()
            _isLoading.value = false
            _radioState.value = RadioPlaybackState.ERROR
            _radioError.value = "No se pudo reproducir la señal en vivo. Verifica tu conexión a internet."
            return
        }
        when {
            controller.mediaItemCount == 0 || controller.playbackState == Player.STATE_IDLE -> {
                if (!pendingPlayOnConnect && _radioState.value != RadioPlaybackState.ERROR) {
                    _radioState.value = RadioPlaybackState.STOPPED
                }
            }
            controller.isPlaying -> {
                radioConnectTimeoutJob?.cancel()
                _isLoading.value = false
                _radioError.value = null
                _radioState.value = RadioPlaybackState.PLAYING
            }
            controller.playbackState == Player.STATE_BUFFERING && controller.playWhenReady -> {
                _radioState.value = RadioPlaybackState.BUFFERING
            }
            controller.playbackState == Player.STATE_READY && !controller.playWhenReady -> {
                radioConnectTimeoutJob?.cancel()
                _isLoading.value = false
                _radioState.value = RadioPlaybackState.PAUSED
            }
            controller.playbackState == Player.STATE_ENDED -> {
                _radioState.value = RadioPlaybackState.STOPPED
            }
        }
    }

    private fun connectRadioController() {
        if (controllerFuture != null) return
        val app = getApplication<Application>()
        val sessionToken = SessionToken(app, ComponentName(app, RadioPlaybackService::class.java))
        val future = MediaController.Builder(app, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val controller = runCatching { future.get() }.getOrNull()
                if (controller == null) {
                    controllerFuture = null
                    radioConnectTimeoutJob?.cancel()
                    _isLoading.value = false
                    if (pendingPlayOnConnect) {
                        pendingPlayOnConnect = false
                        _radioState.value = RadioPlaybackState.ERROR
                        _radioError.value = "No se pudo conectar con el servicio de audio."
                    }
                    return@addListener
                }
                mediaController = controller
                controller.addListener(radioPlayerListener)
                if (pendingPlayOnConnect) {
                    pendingPlayOnConnect = false
                    startStreamOnController(controller)
                } else {
                    if (controller.mediaItemCount > 0) {
                        _radioVolume.value = controller.volume.coerceIn(0f, 1f)
                    } else {
                        controller.volume = _radioVolume.value
                    }
                    syncRadioStateFromController(controller)
                }
            },
            ContextCompat.getMainExecutor(app)
        )
    }

    override fun onCleared() {
        radioConnectTimeoutJob?.cancel()
        mediaController?.removeListener(radioPlayerListener)
        mediaController = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onCleared()
    }
}
