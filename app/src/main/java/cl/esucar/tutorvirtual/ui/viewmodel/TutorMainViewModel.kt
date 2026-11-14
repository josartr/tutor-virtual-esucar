package cl.esucar.tutorvirtual.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.esucar.tutorvirtual.domain.model.Deliverable
import cl.esucar.tutorvirtual.domain.model.DeliverableStatus
import cl.esucar.tutorvirtual.domain.model.PomodoroMode
import cl.esucar.tutorvirtual.domain.model.ProjectTopic
import cl.esucar.tutorvirtual.domain.model.RadioPlaybackState
import cl.esucar.tutorvirtual.domain.model.UserProfile
import cl.esucar.tutorvirtual.domain.model.UserRole
import cl.esucar.tutorvirtual.domain.model.buildFullName
import cl.esucar.tutorvirtual.domain.model.defaultEsucarTutors
import cl.esucar.tutorvirtual.domain.model.defaultRanks
import cl.esucar.tutorvirtual.domain.model.demoStages
import cl.esucar.tutorvirtual.domain.model.institutionForEmail
import cl.esucar.tutorvirtual.domain.model.resolveProfileRank
import cl.esucar.tutorvirtual.domain.repository.AcademicRepository
import cl.esucar.tutorvirtual.domain.repository.AuthRepository
import cl.esucar.tutorvirtual.domain.repository.RadioPlayerGateway
import cl.esucar.tutorvirtual.domain.usecase.CalculateProgressUseCase
import cl.esucar.tutorvirtual.ui.state.MainUiEvent
import cl.esucar.tutorvirtual.ui.state.MainUiState
import cl.esucar.tutorvirtual.ui.state.ProfileEditorUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TutorMainViewModel(
    private val authRepository: AuthRepository,
    private val academicRepository: AcademicRepository,
    private val radioPlayerGateway: RadioPlayerGateway,
    private val calculateProgressUseCase: CalculateProgressUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val customScope: CoroutineScope? = null
) : ViewModel() {

    private val activeScope: CoroutineScope
        get() = customScope ?: viewModelScope

    private val initialProfile = authRepository.currentProfile.value
    private val initialDeliverables = academicRepository.loadDeliverables()
    private val initialChecklist = academicRepository.loadChecklist()
    private val initialNotes = academicRepository.loadStageNotes()
    private val initialTopics = loadOrSeedProjectTopics(initialProfile.researchTopic)

    private val _uiState = MutableStateFlow(
        MainUiState(
            userProfile = initialProfile,
            stages = demoStages,
            deliverables = initialDeliverables,
            checklist = initialChecklist,
            stageNotes = initialNotes,
            projectTopics = initialTopics,
            progressSummary = calculateProgressUseCase(initialDeliverables, demoStages, initialChecklist),
            radioState = radioPlayerGateway.radioState.value,
            radioError = radioPlayerGateway.radioError.value,
            radioVolume = radioPlayerGateway.radioVolume.value
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MainUiEvent>()
    val events: SharedFlow<MainUiEvent> = _events.asSharedFlow()

    private var pomodoroJob: Job? = null
    private var radioConnectTimeoutJob: Job? = null
    private var lastNonZeroVolume: Float = 0.85f

    init {
        observeUserProfile()
        observeRadioGateway()
    }

    private fun observeUserProfile() {
        authRepository.currentProfile
            .onEach { profile ->
                _uiState.update { it.copy(userProfile = profile) }
                if (profile.isLoggedIn) {
                    seedInitialTopicIfNeeded(profile.researchTopic)
                }
            }
            .launchIn(activeScope)
    }

    private fun observeRadioGateway() {
        radioPlayerGateway.radioState
            .onEach { state ->
                _uiState.update { current ->
                    val shouldHideRadioLoading = (state == RadioPlaybackState.PLAYING ||
                        state == RadioPlaybackState.PAUSED ||
                        state == RadioPlaybackState.ERROR ||
                        state == RadioPlaybackState.STOPPED) &&
                        current.loadingMessage == RADIO_CONNECTING_MESSAGE
                    if (shouldHideRadioLoading) {
                        radioConnectTimeoutJob?.cancel()
                    }
                    current.copy(
                        radioState = state,
                        isLoading = if (shouldHideRadioLoading) false else current.isLoading
                    )
                }
            }
            .launchIn(activeScope)

        radioPlayerGateway.radioError
            .onEach { error ->
                _uiState.update { it.copy(radioError = error) }
            }
            .launchIn(activeScope)

        radioPlayerGateway.radioVolume
            .onEach { volume ->
                if (volume > 0.01f) lastNonZeroVolume = volume
                _uiState.update { it.copy(radioVolume = volume) }
            }
            .launchIn(activeScope)
    }

    // --- Navegación externa y notificaciones (One-Shot Events) ---

    fun openExternalUrl(url: String, loadingMessage: String = "Abriendo en el navegador...") {
        activeScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = loadingMessage) }
            try {
                delay(260L)
                _events.emit(MainUiEvent.OpenExternalUrl(url))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun showToast(message: String) {
        activeScope.launch {
            _events.emit(MainUiEvent.ShowToast(message))
        }
    }

    fun onTriggerDeadlineNotificationClicked() {
        activeScope.launch {
            _events.emit(MainUiEvent.TriggerDeadlineNotification)
        }
    }

    // --- Perfil y Sesión ---

    fun openProfileEditor() {
        val profile = _uiState.value.userProfile
        val fallbackParts = profile.fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        _uiState.update {
            it.copy(
                profileEditor = ProfileEditorUiState(
                    isOpen = true,
                    userRole = profile.userRole,
                    rank = if (profile.rank in defaultRanks.drop(1)) profile.rank else defaultRanks[1],
                    firstName = profile.firstName.ifBlank { fallbackParts.firstOrNull().orEmpty() },
                    firstLastName = profile.firstLastName.ifBlank { fallbackParts.getOrNull(1).orEmpty() },
                    secondLastName = profile.secondLastName.ifBlank { fallbackParts.drop(2).joinToString(" ") },
                    email = profile.email,
                    tutorName = if (profile.tutorName.isBlank()) defaultEsucarTutors.first() else profile.tutorName,
                    researchTopic = profile.researchTopic,
                    ssoProvider = profile.ssoProvider,
                    fallbackInstitution = profile.institutionType,
                    submitted = false
                )
            )
        }
    }

    fun dismissProfileEditor() {
        _uiState.update { it.copy(profileEditor = it.profileEditor.copy(isOpen = false)) }
    }

    fun onProfileRoleSelected(role: UserRole) {
        _uiState.update { it.copy(profileEditor = it.profileEditor.copy(userRole = role)) }
    }

    fun onProfileRankSelected(rank: String) {
        _uiState.update { it.copy(profileEditor = it.profileEditor.copy(rank = rank)) }
    }

    fun onProfileFirstNameChanged(value: String) {
        _uiState.update { it.copy(profileEditor = it.profileEditor.copy(firstName = value)) }
    }

    fun onProfileFirstLastNameChanged(value: String) {
        _uiState.update { it.copy(profileEditor = it.profileEditor.copy(firstLastName = value)) }
    }

    fun onProfileSecondLastNameChanged(value: String) {
        _uiState.update { it.copy(profileEditor = it.profileEditor.copy(secondLastName = value)) }
    }

    fun onProfileEmailChanged(value: String) {
        _uiState.update { it.copy(profileEditor = it.profileEditor.copy(email = value)) }
    }

    fun onProfileTutorSelected(tutor: String) {
        _uiState.update { it.copy(profileEditor = it.profileEditor.copy(tutorName = tutor)) }
    }

    fun saveProfileChanges() {
        val editor = _uiState.value.profileEditor.copy(submitted = true)
        _uiState.update { it.copy(profileEditor = editor) }

        if (!editor.validFirstName || !editor.validFirstLastName || !editor.validSecondLastName || !editor.validEmail) {
            return
        }

        val currentProfile = _uiState.value.userProfile
        val cleanFirst = editor.firstName.trim()
        val cleanLast1 = editor.firstLastName.trim()
        val cleanLast2 = editor.secondLastName.trim()
        val cleanEmail = editor.email.trim()
        val resolvedInstitution = institutionForEmail(cleanEmail) ?: currentProfile.institutionType
        val resolvedRank = resolveProfileRank(resolvedInstitution, editor.userRole, editor.rank)

        val updatedProfile = currentProfile.copy(
            rank = resolvedRank,
            firstName = cleanFirst,
            firstLastName = cleanLast1,
            secondLastName = cleanLast2,
            fullName = buildFullName(cleanFirst, cleanLast1, cleanLast2).ifBlank { currentProfile.fullName },
            email = cleanEmail,
            tutorName = editor.tutorName,
            institutionType = resolvedInstitution,
            userRole = editor.userRole
        )

        activeScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Guardando cambios del perfil...") }
            withContext(ioDispatcher) {
                delay(200L)
                authRepository.saveUserProfile(updatedProfile)
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    userProfile = updatedProfile,
                    profileEditor = it.profileEditor.copy(isOpen = false)
                )
            }
            _events.emit(MainUiEvent.ShowToast("Perfil actualizado"))
        }
    }

    fun logoutUser() {
        stopRadio()
        pausePomodoro()
        authRepository.clearSession()
        _uiState.update {
            it.copy(
                userProfile = UserProfile(isLoggedIn = false),
                profileEditor = it.profileEditor.copy(isOpen = false)
            )
        }
        activeScope.launch {
            _events.emit(MainUiEvent.LoggedOut)
        }
    }

    // --- Registro Inmutable de Temas ---

    fun openAddTopicDialog() {
        _uiState.update {
            it.copy(
                topicRegistry = it.topicRegistry.copy(
                    isAddDialogOpen = true,
                    newTopicText = "",
                    submitted = false
                )
            )
        }
    }

    fun dismissAddTopicDialog() {
        _uiState.update {
            it.copy(topicRegistry = it.topicRegistry.copy(isAddDialogOpen = false))
        }
    }

    fun onNewTopicTextChanged(value: String) {
        _uiState.update {
            it.copy(topicRegistry = it.topicRegistry.copy(newTopicText = value))
        }
    }

    fun confirmAddTopic() {
        val currentRegistry = _uiState.value.topicRegistry.copy(submitted = true)
        _uiState.update { it.copy(topicRegistry = currentRegistry) }
        if (!currentRegistry.validNewTopic) return

        val clean = currentRegistry.newTopicText.trim()
        val topic = ProjectTopic(
            id = "topic-${System.currentTimeMillis()}",
            title = clean,
            createdAt = formatTopicTimestamp()
        )
        academicRepository.addProjectTopic(topic)
        val updatedTopics = academicRepository.loadProjectTopics()

        var updatedProfile = _uiState.value.userProfile
        if (updatedProfile.researchTopic.isBlank()) {
            updatedProfile = updatedProfile.copy(researchTopic = clean)
            authRepository.saveUserProfile(updatedProfile)
        }

        _uiState.update {
            it.copy(
                projectTopics = updatedTopics,
                userProfile = updatedProfile,
                topicRegistry = it.topicRegistry.copy(isAddDialogOpen = false, newTopicText = "", submitted = false)
            )
        }
    }

    fun requestDeleteTopic(topic: ProjectTopic) {
        _uiState.update {
            it.copy(topicRegistry = it.topicRegistry.copy(deletingTopic = topic))
        }
    }

    fun dismissDeleteTopic() {
        _uiState.update {
            it.copy(topicRegistry = it.topicRegistry.copy(deletingTopic = null))
        }
    }

    fun confirmDeleteTopic() {
        val target = _uiState.value.topicRegistry.deletingTopic ?: return
        academicRepository.deleteProjectTopic(target.id)
        val updatedTopics = academicRepository.loadProjectTopics()
        val nextTopic = updatedTopics.firstOrNull()?.title.orEmpty()

        var updatedProfile = _uiState.value.userProfile
        if (updatedProfile.researchTopic != nextTopic) {
            updatedProfile = updatedProfile.copy(researchTopic = nextTopic)
            authRepository.saveUserProfile(updatedProfile)
        }

        _uiState.update {
            it.copy(
                projectTopics = updatedTopics,
                userProfile = updatedProfile,
                topicRegistry = it.topicRegistry.copy(deletingTopic = null)
            )
        }
    }

    // --- Hitos / Entregables ---

    fun openAddDeliverableDialog() {
        _uiState.update {
            it.copy(
                deliverableEditor = it.deliverableEditor.copy(
                    isEditorOpen = true,
                    editingItem = null,
                    title = "",
                    dueDate = "",
                    submitted = false
                )
            )
        }
    }

    fun openEditDeliverableDialog(item: Deliverable) {
        _uiState.update {
            it.copy(
                deliverableEditor = it.deliverableEditor.copy(
                    isEditorOpen = true,
                    editingItem = item,
                    title = item.title,
                    dueDate = item.dueDate,
                    submitted = false
                )
            )
        }
    }

    fun dismissDeliverableDialog() {
        _uiState.update {
            it.copy(deliverableEditor = it.deliverableEditor.copy(isEditorOpen = false, editingItem = null))
        }
    }

    fun onDeliverableTitleChanged(value: String) {
        _uiState.update {
            it.copy(deliverableEditor = it.deliverableEditor.copy(title = value))
        }
    }

    fun onDeliverableDueDateChanged(value: String) {
        _uiState.update {
            it.copy(deliverableEditor = it.deliverableEditor.copy(dueDate = value))
        }
    }

    fun confirmSaveDeliverable() {
        val editor = _uiState.value.deliverableEditor.copy(submitted = true)
        _uiState.update { it.copy(deliverableEditor = editor) }
        if (!editor.validTitle || !editor.validDate) return

        val cleanTitle = editor.title.trim()
        val cleanDate = editor.dueDate.trim()
        val currentList = _uiState.value.deliverables

        val updatedList = if (editor.editingItem == null) {
            currentList + Deliverable(
                id = "custom-${System.currentTimeMillis()}",
                title = cleanTitle,
                dueDate = cleanDate,
                status = DeliverableStatus.TODO,
                stageId = "planteamiento"
            )
        } else {
            currentList.map {
                if (it.id == editor.editingItem.id) {
                    it.copy(title = cleanTitle, dueDate = cleanDate)
                } else {
                    it
                }
            }
        }

        persistDeliverablesAndProgress(updatedList)
        _uiState.update {
            it.copy(
                deliverableEditor = it.deliverableEditor.copy(
                    isEditorOpen = false,
                    editingItem = null,
                    title = "",
                    dueDate = "",
                    submitted = false
                )
            )
        }
    }

    fun updateDeliverableStatus(id: String, status: DeliverableStatus) {
        val updated = _uiState.value.deliverables.map {
            if (it.id == id) it.copy(status = status) else it
        }
        persistDeliverablesAndProgress(updated)
    }

    fun requestDeleteDeliverable(item: Deliverable) {
        _uiState.update {
            it.copy(deliverableEditor = it.deliverableEditor.copy(deletingItem = item))
        }
    }

    fun dismissDeleteDeliverable() {
        _uiState.update {
            it.copy(deliverableEditor = it.deliverableEditor.copy(deletingItem = null))
        }
    }

    fun confirmDeleteDeliverable() {
        val target = _uiState.value.deliverableEditor.deletingItem ?: return
        val updated = _uiState.value.deliverables.filterNot { it.id == target.id }
        persistDeliverablesAndProgress(updated)
        _uiState.update {
            it.copy(deliverableEditor = it.deliverableEditor.copy(deletingItem = null))
        }
    }

    // --- Etapas, Checklist y Bitácora ---

    fun setChecklistItem(stageId: String, index: Int, completed: Boolean) {
        val current = _uiState.value.checklist.toMutableMap()
        val items = current[stageId].orEmpty().toMutableSet()
        if (completed) items.add(index) else items.remove(index)
        current[stageId] = items.toSet()

        academicRepository.saveChecklist(current)
        _uiState.update {
            it.copy(
                checklist = current,
                progressSummary = calculateProgressUseCase(it.deliverables, it.stages, current)
            )
        }
    }

    fun saveStageNote(stageId: String, note: String) {
        val updated = _uiState.value.stageNotes.toMutableMap()
        updated[stageId] = note
        academicRepository.saveStageNote(stageId, note)
        _uiState.update { it.copy(stageNotes = updated) }
    }

    // --- Herramientas de Estudio: Pomodoro & APA 7 ---

    fun startPomodoro() {
        if (_uiState.value.pomodoro.isRunning) return
        _uiState.update { it.copy(pomodoro = it.pomodoro.copy(isRunning = true)) }
        pomodoroJob?.cancel()
        pomodoroJob = activeScope.launch {
            while (_uiState.value.pomodoro.isRunning && _uiState.value.pomodoro.secondsRemaining > 0) {
                delay(1000L)
                _uiState.update { state ->
                    val nextSeconds = (state.pomodoro.secondsRemaining - 1).coerceAtLeast(0)
                    state.copy(pomodoro = state.pomodoro.copy(secondsRemaining = nextSeconds))
                }
            }
            if (_uiState.value.pomodoro.secondsRemaining == 0) {
                val nextMode = if (_uiState.value.pomodoro.mode == PomodoroMode.STUDY) {
                    PomodoroMode.REST
                } else {
                    PomodoroMode.STUDY
                }
                _uiState.update {
                    it.copy(
                        pomodoro = it.pomodoro.copy(
                            isRunning = false,
                            mode = nextMode,
                            secondsRemaining = nextMode.defaultSeconds
                        )
                    )
                }
            }
        }
    }

    fun pausePomodoro() {
        pomodoroJob?.cancel()
        _uiState.update { it.copy(pomodoro = it.pomodoro.copy(isRunning = false)) }
    }

    fun resetPomodoro() {
        pausePomodoro()
        _uiState.update { state ->
            state.copy(
                pomodoro = state.pomodoro.copy(
                    secondsRemaining = state.pomodoro.mode.defaultSeconds
                )
            )
        }
    }

    fun onApaTypeSelected(type: String) {
        _uiState.update { it.copy(apaGenerator = it.apaGenerator.copy(selectedType = type)) }
    }

    fun onApaAuthorChanged(value: String) {
        _uiState.update { it.copy(apaGenerator = it.apaGenerator.copy(author = value)) }
    }

    fun onApaYearChanged(value: String) {
        _uiState.update { it.copy(apaGenerator = it.apaGenerator.copy(year = value)) }
    }

    fun onApaTitleChanged(value: String) {
        _uiState.update { it.copy(apaGenerator = it.apaGenerator.copy(title = value)) }
    }

    fun onApaSourceChanged(value: String) {
        _uiState.update { it.copy(apaGenerator = it.apaGenerator.copy(source = value)) }
    }

    fun copyGeneratedApaCitation() {
        val citation = _uiState.value.apaGenerator.generatedCitation
        activeScope.launch {
            _events.emit(MainUiEvent.CopyToClipboard("Cita APA 7", citation))
        }
    }

    // --- Preguntas Frecuentes (FAQ) ---

    fun onFaqQueryChanged(query: String) {
        _uiState.update { it.copy(faq = it.faq.copy(query = query)) }
    }

    fun onFaqCategorySelected(category: String) {
        _uiState.update { it.copy(faq = it.faq.copy(selectedCategory = category)) }
    }

    // --- Radio Carabineros (delegado a RadioPlayerGateway) ---

    fun onRadioQuickActionClicked() {
        val state = _uiState.value.radioState
        if (state == RadioPlaybackState.STOPPED || state == RadioPlaybackState.ERROR) {
            activeScope.launch {
                _events.emit(MainUiEvent.RequestRadioWithNotificationPermission)
            }
        } else {
            toggleRadio()
        }
    }

    fun toggleRadio() {
        when (_uiState.value.radioState) {
            RadioPlaybackState.PLAYING -> pauseRadio()
            RadioPlaybackState.PAUSED -> resumeRadio()
            RadioPlaybackState.BUFFERING -> stopRadio()
            RadioPlaybackState.STOPPED,
            RadioPlaybackState.ERROR -> playRadio()
        }
    }

    fun playRadio() {
        _uiState.update {
            it.copy(
                isLoading = true,
                loadingMessage = RADIO_CONNECTING_MESSAGE,
                radioError = null
            )
        }
        radioConnectTimeoutJob?.cancel()
        radioConnectTimeoutJob = activeScope.launch {
            delay(3000L)
            _uiState.update { state ->
                if (state.isLoading && state.loadingMessage == RADIO_CONNECTING_MESSAGE) {
                    state.copy(isLoading = false)
                } else {
                    state
                }
            }
        }
        radioPlayerGateway.play()
    }

    fun pauseRadio() {
        radioConnectTimeoutJob?.cancel()
        _uiState.update { state ->
            if (state.loadingMessage == RADIO_CONNECTING_MESSAGE) state.copy(isLoading = false) else state
        }
        radioPlayerGateway.pause()
    }

    fun resumeRadio() {
        radioPlayerGateway.resume()
    }

    fun stopRadio() {
        radioConnectTimeoutJob?.cancel()
        _uiState.update { state ->
            if (state.loadingMessage == RADIO_CONNECTING_MESSAGE) state.copy(isLoading = false) else state
        }
        radioPlayerGateway.stop()
    }

    fun setRadioVolume(volume: Float) {
        if (volume > 0.01f) lastNonZeroVolume = volume
        radioPlayerGateway.setVolume(volume)
    }

    fun toggleRadioMute() {
        val current = _uiState.value.radioVolume
        if (current > 0f) {
            lastNonZeroVolume = current
            radioPlayerGateway.setVolume(0f)
        } else {
            val restore = if (lastNonZeroVolume > 0.05f) lastNonZeroVolume else 0.85f
            radioPlayerGateway.setVolume(restore)
        }
    }

    // --- Helpers internos ---

    private fun persistDeliverablesAndProgress(updated: List<Deliverable>) {
        academicRepository.saveDeliverables(updated)
        _uiState.update {
            it.copy(
                deliverables = updated,
                progressSummary = calculateProgressUseCase(updated, it.stages, it.checklist)
            )
        }
    }

    private fun loadOrSeedProjectTopics(initialTopic: String): List<ProjectTopic> {
        val current = academicRepository.loadProjectTopics()
        val clean = initialTopic.trim()
        if (current.isEmpty() && clean.length >= 4) {
            val seeded = ProjectTopic(
                id = "topic-initial-${System.currentTimeMillis()}",
                title = clean,
                createdAt = formatTopicTimestamp()
            )
            academicRepository.addProjectTopic(seeded)
            return listOf(seeded)
        }
        return current
    }

    private fun seedInitialTopicIfNeeded(topicTitle: String) {
        val clean = topicTitle.trim()
        if (clean.length < 4) return
        val existing = academicRepository.loadProjectTopics()
        val updated = if (existing.none { it.title.equals(clean, ignoreCase = true) }) {
            val newTopic = ProjectTopic(
                id = "topic-${System.currentTimeMillis()}",
                title = clean,
                createdAt = formatTopicTimestamp()
            )
            academicRepository.addProjectTopic(newTopic)
            academicRepository.loadProjectTopics()
        } else {
            existing
        }
        _uiState.update { it.copy(projectTopics = updated) }
    }

    private fun formatTopicTimestamp(): String =
        SimpleDateFormat("dd MMM yyyy · HH:mm", Locale("es", "ES")).format(Date())

    override fun onCleared() {
        pomodoroJob?.cancel()
        radioConnectTimeoutJob?.cancel()
        radioPlayerGateway.release()
        super.onCleared()
    }

    companion object {
        private const val RADIO_CONNECTING_MESSAGE = "Conectando con Radio Carabineros..."
    }
}
