package cl.esucar.tutorvirtual.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.esucar.tutorvirtual.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    fun registerUser(
        rank: String,
        fullName: String,
        email: String,
        researchTopic: String,
        tutorName: String,
        institutionType: InstitutionType,
        password: String
    ): AuthResult {
        val profile = UserProfile(
            rank = if (institutionType == InstitutionType.CARABINEROS) rank else "Alumno ESUCAR",
            fullName = fullName.trim(),
            email = email.trim(),
            researchTopic = researchTopic.trim(),
            tutorName = tutorName.trim(),
            ssoProvider = "Registro institucional",
            institutionType = institutionType,
            isLoggedIn = true
        )
        return when (val result = repository.registerUser(profile, password)) {
            is AuthResult.Success -> {
                _userProfile.value = result.profile
                result
            }
            else -> result
        }
    }

    fun login(email: String, password: String): AuthResult {
        return when (val result = repository.login(email, password)) {
            is AuthResult.Success -> {
                _userProfile.value = result.profile
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
            fullName = "Marcos Soto R.",
            email = "marcos.soto@carabineros.cl",
            researchTopic = "Estrategias de Seguridad Preventiva Cuadrante",
            tutorName = "Mayor R. González",
            ssoProvider = provider,
            isLoggedIn = true
        )
        _userProfile.value = defaultProfile
        repository.saveUserProfile(defaultProfile)
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

    fun logoutUser() {
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
}
