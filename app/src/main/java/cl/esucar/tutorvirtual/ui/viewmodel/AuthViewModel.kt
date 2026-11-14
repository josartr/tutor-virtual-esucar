package cl.esucar.tutorvirtual.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.esucar.tutorvirtual.domain.model.AuthResult
import cl.esucar.tutorvirtual.domain.model.InstitutionType
import cl.esucar.tutorvirtual.domain.model.UserProfile
import cl.esucar.tutorvirtual.domain.model.UserRole
import cl.esucar.tutorvirtual.domain.model.buildFullName
import cl.esucar.tutorvirtual.domain.model.generateInstitutionalEmail
import cl.esucar.tutorvirtual.domain.model.institutionForEmail
import cl.esucar.tutorvirtual.domain.model.resolveProfileRank
import cl.esucar.tutorvirtual.domain.repository.AuthRepository
import cl.esucar.tutorvirtual.ui.state.AuthScreenStatus
import cl.esucar.tutorvirtual.ui.state.AuthUiEvent
import cl.esucar.tutorvirtual.ui.state.AuthUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val customScope: CoroutineScope? = null
) : ViewModel() {

    private val activeScope: CoroutineScope
        get() = customScope ?: viewModelScope

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthUiEvent>()
    val events: SharedFlow<AuthUiEvent> = _events.asSharedFlow()

    fun onModeChanged(registrationMode: Boolean) {
        _uiState.update { state ->
            val updated = state.copy(
                registrationMode = registrationMode,
                submitted = false,
                status = AuthScreenStatus.IDLE,
                feedbackMessage = null,
                feedbackIsError = false
            )
            if (registrationMode && updated.email.isBlank() &&
                (updated.firstName.isNotBlank() || updated.firstLastName.isNotBlank())
            ) {
                applyAutoEmail(updated, forceRegenerate = true)
            } else {
                updated
            }
        }
    }

    fun onUserRoleSelected(role: UserRole) {
        _uiState.update { state ->
            applyAutoEmail(state.copy(userRole = role), forceRegenerate = false)
        }
    }

    fun onRankSelected(rank: String) {
        _uiState.update { it.copy(rank = rank) }
    }

    fun onTutorSelected(tutor: String) {
        _uiState.update { it.copy(tutor = tutor) }
    }

    fun onFirstNameChanged(value: String) {
        _uiState.update { state ->
            applyAutoEmail(state.copy(firstName = value), forceRegenerate = false)
        }
    }

    fun onFirstLastNameChanged(value: String) {
        _uiState.update { state ->
            applyAutoEmail(state.copy(firstLastName = value), forceRegenerate = false)
        }
    }

    fun onSecondLastNameChanged(value: String) {
        _uiState.update { state ->
            applyAutoEmail(state.copy(secondLastName = value), forceRegenerate = false)
        }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { state ->
            if (!state.registrationMode) {
                state.copy(email = value)
            } else {
                val nextRole = when (institutionForEmail(value)) {
                    InstitutionType.CARABINEROS -> UserRole.FUNCIONARIO
                    InstitutionType.ESUCAR -> UserRole.ESTUDIANTE
                    null -> state.userRole
                }
                state.copy(
                    email = value,
                    emailAutoManaged = value.isBlank(),
                    userRole = nextRole
                )
            }
        }
    }

    fun onRegenerateEmail() {
        _uiState.update { state ->
            applyAutoEmail(state, forceRegenerate = true)
        }
    }

    fun onApplyDomainSuggestion(domain: String) {
        _uiState.update { state ->
            val localPart = state.email.trim().substringBefore("@").ifBlank { "nombre.apellido" }
            val nextEmail = "$localPart@$domain"
            if (state.registrationMode) {
                state.copy(
                    email = nextEmail,
                    emailAutoManaged = false,
                    userRole = if (domain == "carabineros.cl") UserRole.FUNCIONARIO else UserRole.ESTUDIANTE
                )
            } else {
                state.copy(email = nextEmail)
            }
        }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmPassword = value) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun onToggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
    }

    fun onTopicChanged(value: String) {
        _uiState.update { it.copy(researchTopic = value) }
    }

    fun onSubmit() {
        val current = _uiState.value
        _uiState.update {
            it.copy(
                submitted = true,
                feedbackMessage = null,
                feedbackIsError = false
            )
        }

        if (!current.validEmail || !current.validPassword) return

        if (!current.registrationMode) {
            performLogin(current.email, current.password)
            return
        }

        val resolvedInstitution = current.detectedInstitution ?: return
        if (!current.validFirstName ||
            !current.validFirstLastName ||
            !current.validSecondLastName ||
            !current.validTopic ||
            !current.validRank ||
            !current.passwordsMatch
        ) {
            return
        }

        performRegister(current, resolvedInstitution)
    }

    fun onQuickDemoLogin(provider: String = "SSO Institucional (Demo)") {
        activeScope.launch {
            _uiState.update {
                it.copy(
                    status = AuthScreenStatus.LOADING,
                    loadingMessage = "Preparando acceso institucional...",
                    feedbackMessage = null,
                    feedbackIsError = false
                )
            }
            val profile = withContext(ioDispatcher) {
                delay(280L)
                authRepository.loginQuickSso(provider)
            }
            _uiState.update { it.copy(status = AuthScreenStatus.SUCCESS) }
            _events.emit(AuthUiEvent.Authenticated(profile))
        }
    }

    fun clearSensitiveFieldsAfterLogout() {
        _uiState.value = AuthUiState()
    }

    private fun performLogin(email: String, password: String) {
        activeScope.launch {
            _uiState.update {
                it.copy(
                    status = AuthScreenStatus.LOADING,
                    loadingMessage = "Verificando credenciales...",
                    feedbackMessage = null,
                    feedbackIsError = false
                )
            }
            val result = withContext(ioDispatcher) {
                delay(250L)
                runCatching { authRepository.login(email, password) }
                    .getOrElse { AuthResult.PersistenceError }
            }
            handleAuthResult(result)
        }
    }

    private fun performRegister(state: AuthUiState, resolvedInstitution: InstitutionType) {
        val cleanFirst = state.firstName.trim()
        val cleanLast1 = state.firstLastName.trim()
        val cleanLast2 = state.secondLastName.trim()
        val profile = UserProfile(
            rank = resolveProfileRank(resolvedInstitution, state.userRole, state.rank),
            firstName = cleanFirst,
            firstLastName = cleanLast1,
            secondLastName = cleanLast2,
            fullName = buildFullName(cleanFirst, cleanLast1, cleanLast2),
            email = state.email.trim(),
            researchTopic = state.researchTopic.trim(),
            tutorName = state.tutor.trim(),
            ssoProvider = "Registro institucional",
            institutionType = resolvedInstitution,
            userRole = state.userRole,
            isLoggedIn = true
        )

        activeScope.launch {
            _uiState.update {
                it.copy(
                    status = AuthScreenStatus.LOADING,
                    loadingMessage = "Creando cuenta institucional...",
                    feedbackMessage = null,
                    feedbackIsError = false
                )
            }
            val result = withContext(ioDispatcher) {
                delay(250L)
                runCatching { authRepository.registerUser(profile, state.password) }
                    .getOrElse { AuthResult.PersistenceError }
            }
            handleAuthResult(result)
        }
    }

    private suspend fun handleAuthResult(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                _uiState.update {
                    it.copy(
                        status = AuthScreenStatus.SUCCESS,
                        password = "",
                        confirmPassword = "",
                        feedbackMessage = null,
                        feedbackIsError = false
                    )
                }
                _events.emit(AuthUiEvent.Authenticated(result.profile))
            }
            AuthResult.InvalidCredentials -> {
                val msg = "Correo o contraseña incorrectos."
                _uiState.update {
                    it.copy(
                        status = AuthScreenStatus.ERROR,
                        feedbackMessage = msg,
                        feedbackIsError = true
                    )
                }
                _events.emit(AuthUiEvent.ShowFeedback(msg, isError = true))
            }
            AuthResult.EmailAlreadyRegistered -> {
                val msg = "Este correo ya está registrado. Inicia sesión para continuar."
                _uiState.update {
                    it.copy(
                        status = AuthScreenStatus.ERROR,
                        feedbackMessage = msg,
                        feedbackIsError = true
                    )
                }
                _events.emit(AuthUiEvent.ShowFeedback(msg, isError = true))
            }
            AuthResult.InvalidRegistration -> {
                val msg = "Revisa los datos del registro."
                _uiState.update {
                    it.copy(
                        status = AuthScreenStatus.ERROR,
                        feedbackMessage = msg,
                        feedbackIsError = true
                    )
                }
                _events.emit(AuthUiEvent.ShowFeedback(msg, isError = true))
            }
            AuthResult.PersistenceError -> {
                val msg = "No fue posible acceder a la base de datos. Intenta nuevamente."
                _uiState.update {
                    it.copy(
                        status = AuthScreenStatus.ERROR,
                        feedbackMessage = msg,
                        feedbackIsError = true
                    )
                }
                _events.emit(AuthUiEvent.ShowFeedback(msg, isError = true))
            }
        }
    }

    private fun applyAutoEmail(state: AuthUiState, forceRegenerate: Boolean): AuthUiState {
        if (!state.registrationMode) return state
        return if (forceRegenerate || state.emailAutoManaged) {
            state.copy(
                emailAutoManaged = true,
                email = generateInstitutionalEmail(
                    state.firstName,
                    state.firstLastName,
                    state.secondLastName,
                    state.userRole
                )
            )
        } else {
            val localPart = state.email.trim().substringBefore("@")
            if (localPart.isNotBlank()) {
                val domain = if (state.userRole == UserRole.FUNCIONARIO) "carabineros.cl" else "esucar.cl"
                state.copy(email = "$localPart@$domain")
            } else {
                state
            }
        }
    }
}
