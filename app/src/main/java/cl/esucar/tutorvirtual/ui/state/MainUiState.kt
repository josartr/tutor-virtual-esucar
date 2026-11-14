package cl.esucar.tutorvirtual.ui.state

import cl.esucar.tutorvirtual.domain.model.Deliverable
import cl.esucar.tutorvirtual.domain.model.Faq
import cl.esucar.tutorvirtual.domain.model.InstitutionType
import cl.esucar.tutorvirtual.domain.model.PomodoroMode
import cl.esucar.tutorvirtual.domain.model.ProgressSummary
import cl.esucar.tutorvirtual.domain.model.ProjectTopic
import cl.esucar.tutorvirtual.domain.model.RadioPlaybackState
import cl.esucar.tutorvirtual.domain.model.Stage
import cl.esucar.tutorvirtual.domain.model.UserProfile
import cl.esucar.tutorvirtual.domain.model.UserRole
import cl.esucar.tutorvirtual.domain.model.defaultEsucarTutors
import cl.esucar.tutorvirtual.domain.model.defaultRanks
import cl.esucar.tutorvirtual.domain.model.demoFaqs
import cl.esucar.tutorvirtual.domain.model.demoStages
import cl.esucar.tutorvirtual.domain.model.faqCategories
import cl.esucar.tutorvirtual.domain.model.generateApaCitation
import cl.esucar.tutorvirtual.domain.model.institutionForEmail
import cl.esucar.tutorvirtual.domain.model.isValidInstitutionalEmail

data class PomodoroUiState(
    val secondsRemaining: Int = PomodoroMode.STUDY.defaultSeconds,
    val isRunning: Boolean = false,
    val mode: PomodoroMode = PomodoroMode.STUDY
) {
    val formattedTime: String
        get() = "%02d:%02d".format(secondsRemaining / 60, secondsRemaining % 60)
}

data class ApaGeneratorUiState(
    val selectedType: String = "Ley",
    val author: String = "",
    val year: String = "",
    val title: String = "",
    val source: String = "",
    val availableTypes: List<String> = listOf("Ley", "Reglamento", "Libro")
) {
    val generatedCitation: String
        get() = generateApaCitation(selectedType, author, year, title, source)
}

data class FaqUiState(
    val query: String = "",
    val selectedCategory: String = "Todas",
    val categories: List<String> = faqCategories,
    val allFaqs: List<Faq> = demoFaqs
) {
    val filteredFaqs: List<Faq>
        get() = allFaqs.filter { faq ->
            (selectedCategory == "Todas" || faq.category == selectedCategory) &&
                (query.isBlank() || faq.question.contains(query, ignoreCase = true) ||
                    faq.answer.contains(query, ignoreCase = true))
        }
}

data class ProfileEditorUiState(
    val isOpen: Boolean = false,
    val userRole: UserRole = UserRole.ESTUDIANTE,
    val rank: String = defaultRanks[1],
    val firstName: String = "",
    val firstLastName: String = "",
    val secondLastName: String = "",
    val email: String = "",
    val tutorName: String = defaultEsucarTutors.first(),
    val researchTopic: String = "",
    val ssoProvider: String = "",
    val fallbackInstitution: InstitutionType = InstitutionType.ESUCAR,
    val submitted: Boolean = false,
    val availableRanks: List<String> = defaultRanks.drop(1),
    val availableTutors: List<String> = defaultEsucarTutors
) {
    val detectedInstitution: InstitutionType
        get() = institutionForEmail(email) ?: fallbackInstitution

    val showPoliceRank: Boolean
        get() = detectedInstitution == InstitutionType.CARABINEROS

    val validFirstName: Boolean
        get() = firstName.trim().length >= 2

    val validFirstLastName: Boolean
        get() = firstLastName.trim().length >= 2

    val validSecondLastName: Boolean
        get() = secondLastName.trim().length >= 2

    val validEmail: Boolean
        get() = isValidInstitutionalEmail(email)
}

data class TopicRegistryUiState(
    val isAddDialogOpen: Boolean = false,
    val newTopicText: String = "",
    val submitted: Boolean = false,
    val deletingTopic: ProjectTopic? = null
) {
    val validNewTopic: Boolean
        get() = newTopicText.trim().length >= 4
}

data class DeliverableEditorUiState(
    val isEditorOpen: Boolean = false,
    val editingItem: Deliverable? = null,
    val title: String = "",
    val dueDate: String = "",
    val submitted: Boolean = false,
    val deletingItem: Deliverable? = null
) {
    val validTitle: Boolean
        get() = title.trim().length >= 3

    val validDate: Boolean
        get() = dueDate.trim().length >= 4
}

data class MainUiState(
    val userProfile: UserProfile = UserProfile(),
    val stages: List<Stage> = demoStages,
    val deliverables: List<Deliverable> = emptyList(),
    val checklist: Map<String, Set<Int>> = emptyMap(),
    val stageNotes: Map<String, String> = emptyMap(),
    val projectTopics: List<ProjectTopic> = emptyList(),
    val progressSummary: ProgressSummary = ProgressSummary(),
    val pomodoro: PomodoroUiState = PomodoroUiState(),
    val apaGenerator: ApaGeneratorUiState = ApaGeneratorUiState(),
    val faq: FaqUiState = FaqUiState(),
    val radioState: RadioPlaybackState = RadioPlaybackState.STOPPED,
    val radioError: String? = null,
    val radioVolume: Float = 0.85f,
    val profileEditor: ProfileEditorUiState = ProfileEditorUiState(),
    val topicRegistry: TopicRegistryUiState = TopicRegistryUiState(),
    val deliverableEditor: DeliverableEditorUiState = DeliverableEditorUiState(),
    val isLoading: Boolean = false,
    val loadingMessage: String = "Cargando..."
)

sealed interface MainUiEvent {
    data class ShowToast(val message: String) : MainUiEvent
    data class OpenExternalUrl(val url: String) : MainUiEvent
    data class CopyToClipboard(val label: String, val text: String) : MainUiEvent
    data object RequestRadioWithNotificationPermission : MainUiEvent
    data object TriggerDeadlineNotification : MainUiEvent
    data object LoggedOut : MainUiEvent
}
