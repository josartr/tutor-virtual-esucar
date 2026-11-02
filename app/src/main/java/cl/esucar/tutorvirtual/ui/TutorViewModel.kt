package cl.esucar.tutorvirtual.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cl.esucar.tutorvirtual.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TutorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalRepository(application)
    private val _deliverables = MutableStateFlow(repository.loadDeliverables())
    val deliverables: StateFlow<List<Deliverable>> = _deliverables.asStateFlow()
    private val _checklist = MutableStateFlow(repository.loadChecklist())
    val checklist: StateFlow<Map<String, Set<Int>>> = _checklist.asStateFlow()
    val stages = demoStages
    val faqs = demoFaqs

    fun updateStatus(id: String, status: DeliverableStatus) {
        val updated = _deliverables.value.map { if (it.id == id) it.copy(status = status) else it }
        _deliverables.value = updated
        repository.saveDeliverables(updated)
    }

    fun addDeliverable(title: String, dueDate: String) {
        val item = Deliverable("custom-${System.currentTimeMillis()}", title, dueDate, DeliverableStatus.TODO, "planteamiento")
        val updated = _deliverables.value + item
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
}
