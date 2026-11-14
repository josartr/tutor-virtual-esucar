package cl.esucar.tutorvirtual.ui.state

import cl.esucar.tutorvirtual.domain.model.InstitutionType
import cl.esucar.tutorvirtual.domain.model.UserProfile
import cl.esucar.tutorvirtual.domain.model.UserRole
import cl.esucar.tutorvirtual.domain.model.defaultEsucarTutors
import cl.esucar.tutorvirtual.domain.model.defaultRanks
import cl.esucar.tutorvirtual.domain.model.institutionForEmail

enum class AuthScreenStatus {
    IDLE,
    LOADING,
    SUCCESS,
    ERROR
}

data class AuthUiState(
    val registrationMode: Boolean = false,
    val userRole: UserRole = UserRole.ESTUDIANTE,
    val emailAutoManaged: Boolean = true,
    val rank: String = defaultRanks[1],
    val tutor: String = defaultEsucarTutors.first(),
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val firstName: String = "",
    val firstLastName: String = "",
    val secondLastName: String = "",
    val researchTopic: String = "",
    val submitted: Boolean = false,
    val status: AuthScreenStatus = AuthScreenStatus.IDLE,
    val loadingMessage: String = "Cargando...",
    val feedbackMessage: String? = null,
    val feedbackIsError: Boolean = false,
    val availableRanks: List<String> = defaultRanks.drop(1),
    val availableTutors: List<String> = defaultEsucarTutors
) {
    val isLoading: Boolean
        get() = status == AuthScreenStatus.LOADING

    val detectedInstitution: InstitutionType?
        get() = institutionForEmail(email)

    val validEmail: Boolean
        get() = detectedInstitution != null

    val validPassword: Boolean
        get() = password.length >= 8

    val passwordsMatch: Boolean
        get() = password == confirmPassword

    val validFirstName: Boolean
        get() = firstName.trim().length >= 2

    val validFirstLastName: Boolean
        get() = firstLastName.trim().length >= 2

    val validSecondLastName: Boolean
        get() = secondLastName.trim().length >= 2

    val validTopic: Boolean
        get() = researchTopic.trim().length >= 4

    val isCarabinerosDomain: Boolean
        get() = detectedInstitution == InstitutionType.CARABINEROS ||
            (registrationMode && userRole == UserRole.FUNCIONARIO)

    val validRank: Boolean
        get() = !isCarabinerosDomain || rank.isNotBlank()

    val canRegenerateEmail: Boolean
        get() = registrationMode && !emailAutoManaged &&
            (firstName.isNotBlank() || firstLastName.isNotBlank())
}

sealed interface AuthUiEvent {
    data class Authenticated(val profile: UserProfile) : AuthUiEvent
    data class ShowFeedback(val message: String, val isError: Boolean) : AuthUiEvent
}
