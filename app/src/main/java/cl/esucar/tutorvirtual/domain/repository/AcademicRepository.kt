package cl.esucar.tutorvirtual.domain.repository

import cl.esucar.tutorvirtual.domain.model.Deliverable
import cl.esucar.tutorvirtual.domain.model.ProjectTopic

interface AcademicRepository {
    fun loadDeliverables(): List<Deliverable>
    fun saveDeliverables(items: List<Deliverable>)
    fun loadChecklist(): Map<String, Set<Int>>
    fun saveChecklist(checklist: Map<String, Set<Int>>)
    fun loadStageNotes(): Map<String, String>
    fun saveStageNote(stageId: String, note: String)
    fun loadProjectTopics(): List<ProjectTopic>
    fun addProjectTopic(topic: ProjectTopic)
    fun deleteProjectTopic(id: String)
}
