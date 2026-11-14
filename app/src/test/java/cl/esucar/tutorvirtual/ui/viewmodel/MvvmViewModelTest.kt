package cl.esucar.tutorvirtual.ui.viewmodel

import cl.esucar.tutorvirtual.domain.model.AuthResult
import cl.esucar.tutorvirtual.domain.model.Deliverable
import cl.esucar.tutorvirtual.domain.model.DeliverableStatus
import cl.esucar.tutorvirtual.domain.model.ProjectTopic
import cl.esucar.tutorvirtual.domain.model.RadioPlaybackState
import cl.esucar.tutorvirtual.domain.model.UserProfile
import cl.esucar.tutorvirtual.domain.model.UserRole
import cl.esucar.tutorvirtual.domain.model.demoDeliverables
import cl.esucar.tutorvirtual.domain.model.demoStages
import cl.esucar.tutorvirtual.domain.repository.AcademicRepository
import cl.esucar.tutorvirtual.domain.repository.AuthRepository
import cl.esucar.tutorvirtual.domain.repository.RadioPlayerGateway
import cl.esucar.tutorvirtual.domain.usecase.CalculateProgressUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MvvmViewModelTest {

    private class FakeAuthRepository : AuthRepository {
        private val _profile = MutableStateFlow(UserProfile(isLoggedIn = true, researchTopic = "Tema Inicial"))
        override val currentProfile: StateFlow<UserProfile> = _profile.asStateFlow()
        override fun loadUserProfile(): UserProfile = _profile.value
        override fun registerUser(profile: UserProfile, password: String): AuthResult = AuthResult.Success(profile)
        override fun login(emailInput: String, password: String): AuthResult = AuthResult.Success(_profile.value)
        override fun loginQuickSso(provider: String): UserProfile = _profile.value
        override fun saveUserProfile(profile: UserProfile) { _profile.value = profile }
        override fun clearSession() { _profile.value = UserProfile(isLoggedIn = false) }
    }

    private class FakeAcademicRepository : AcademicRepository {
        private var deliverables = demoDeliverables
        private var checklist = demoStages.associate { it.id to emptySet<Int>() }
        private val notes = mutableMapOf<String, String>()
        private val topics = mutableListOf<ProjectTopic>()

        override fun loadDeliverables(): List<Deliverable> = deliverables
        override fun saveDeliverables(items: List<Deliverable>) { deliverables = items }
        override fun loadChecklist(): Map<String, Set<Int>> = checklist
        override fun saveChecklist(checklist: Map<String, Set<Int>>) { this.checklist = checklist }
        override fun loadStageNotes(): Map<String, String> = notes
        override fun saveStageNote(stageId: String, note: String) { notes[stageId] = note }
        override fun loadProjectTopics(): List<ProjectTopic> = topics.toList()
        override fun addProjectTopic(topic: ProjectTopic) { topics.add(0, topic) }
        override fun deleteProjectTopic(id: String) { topics.removeAll { it.id == id } }
    }

    private class FakeRadioGateway : RadioPlayerGateway {
        private val _state = MutableStateFlow(RadioPlaybackState.STOPPED)
        override val radioState: StateFlow<RadioPlaybackState> = _state.asStateFlow()
        private val _error = MutableStateFlow<String?>(null)
        override val radioError: StateFlow<String?> = _error.asStateFlow()
        private val _volume = MutableStateFlow(0.85f)
        override val radioVolume: StateFlow<Float> = _volume.asStateFlow()

        override fun play() { _state.value = RadioPlaybackState.PLAYING }
        override fun pause() { _state.value = RadioPlaybackState.PAUSED }
        override fun resume() { _state.value = RadioPlaybackState.PLAYING }
        override fun stop() { _state.value = RadioPlaybackState.STOPPED }
        override fun setVolume(volume: Float) { _volume.value = volume.coerceIn(0f, 1f) }
        override fun release() {}
    }

    @Test
    fun `auth viewModel generates institutional email automatically by role`() {
        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val vm = AuthViewModel(FakeAuthRepository(), Dispatchers.Unconfined, testScope)
        vm.onModeChanged(true)
        vm.onFirstNameChanged("Marcos")
        vm.onFirstLastNameChanged("Soto")
        vm.onSecondLastNameChanged("Rojas")
        assertEquals("marcos.soto.r@esucar.cl", vm.uiState.value.email)

        vm.onUserRoleSelected(UserRole.FUNCIONARIO)
        assertEquals("marcos.soto.r@carabineros.cl", vm.uiState.value.email)
        assertTrue(vm.uiState.value.isCarabinerosDomain)
    }

    @Test
    fun `main viewModel updates checklist and recalculates progress summary`() {
        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val mainVm = TutorMainViewModel(
            authRepository = FakeAuthRepository(),
            academicRepository = FakeAcademicRepository(),
            radioPlayerGateway = FakeRadioGateway(),
            calculateProgressUseCase = CalculateProgressUseCase(),
            ioDispatcher = Dispatchers.Unconfined,
            customScope = testScope
        )

        val before = mainVm.uiState.value.progressSummary.completedChecklistItems
        mainVm.setChecklistItem("planteamiento", 0, true)
        val after = mainVm.uiState.value.progressSummary.completedChecklistItems

        assertEquals(before + 1, after)
    }

    @Test
    fun `main viewModel manages immutable project topics add and delete`() {
        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val mainVm = TutorMainViewModel(
            authRepository = FakeAuthRepository(),
            academicRepository = FakeAcademicRepository(),
            radioPlayerGateway = FakeRadioGateway(),
            calculateProgressUseCase = CalculateProgressUseCase(),
            ioDispatcher = Dispatchers.Unconfined,
            customScope = testScope
        )

        mainVm.openAddTopicDialog()
        mainVm.onNewTopicTextChanged("Gestión Preventiva Urbana")
        mainVm.confirmAddTopic()

        assertTrue(mainVm.uiState.value.projectTopics.any { it.title == "Gestión Preventiva Urbana" })
        assertFalse(mainVm.uiState.value.topicRegistry.isAddDialogOpen)

        val added = mainVm.uiState.value.projectTopics.first { it.title == "Gestión Preventiva Urbana" }
        mainVm.requestDeleteTopic(added)
        mainVm.confirmDeleteTopic()

        assertFalse(mainVm.uiState.value.projectTopics.any { it.id == added.id })
    }

    @Test
    fun `main viewModel updates deliverable status and filters faq`() {
        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val mainVm = TutorMainViewModel(
            authRepository = FakeAuthRepository(),
            academicRepository = FakeAcademicRepository(),
            radioPlayerGateway = FakeRadioGateway(),
            calculateProgressUseCase = CalculateProgressUseCase(),
            ioDispatcher = Dispatchers.Unconfined,
            customScope = testScope
        )

        val firstId = mainVm.uiState.value.deliverables.first().id
        mainVm.updateDeliverableStatus(firstId, DeliverableStatus.APPROVED)
        assertEquals(DeliverableStatus.APPROVED, mainVm.uiState.value.deliverables.first().status)

        mainVm.onFaqCategorySelected("Citación APA")
        assertTrue(mainVm.uiState.value.faq.filteredFaqs.all { it.category == "Citación APA" })
    }
}
