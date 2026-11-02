package cl.esucar.tutorvirtual.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressCalculatorTest {
    @Test
    fun `progress increases as deliverables advance`() {
        val initial = calculateProgress(
            deliverables = listOf(Deliverable("1", "Hito", "Hoy", DeliverableStatus.TODO, "s")),
            stages = emptyList(),
            checklist = emptyMap()
        )
        val approved = calculateProgress(
            deliverables = listOf(Deliverable("1", "Hito", "Hoy", DeliverableStatus.APPROVED, "s")),
            stages = emptyList(),
            checklist = emptyMap()
        )

        assertEquals(0, initial.percentage)
        assertEquals(100, approved.percentage)
    }

    @Test
    fun `checklist contributes progressively to progress`() {
        val stage = Stage("s", "Etapa", "", "", listOf("Uno", "Dos"), 0L)
        val summary = calculateProgress(
            deliverables = emptyList(),
            stages = listOf(stage),
            checklist = mapOf("s" to setOf(0))
        )

        assertEquals(50, summary.percentage)
        assertEquals(1, summary.completedChecklistItems)
    }
}
