package cl.esucar.tutorvirtual.data

import kotlin.math.roundToInt

data class ProgressSummary(
    val fraction: Float,
    val percentage: Int,
    val approvedDeliverables: Int,
    val completedChecklistItems: Int,
    val totalChecklistItems: Int
)

fun calculateProgress(
    deliverables: List<Deliverable>,
    stages: List<Stage>,
    checklist: Map<String, Set<Int>>
): ProgressSummary {
    val deliverableScore = deliverables.sumOf { deliverable ->
        when (deliverable.status) {
            DeliverableStatus.TODO -> 0.0
            DeliverableStatus.IN_PROGRESS -> 0.35
            DeliverableStatus.SUBMITTED -> 0.75
            DeliverableStatus.APPROVED -> 1.0
        }
    }
    val totalChecklistItems = stages.sumOf { it.checklist.size }
    val completedChecklistItems = stages.sumOf { stage ->
        checklist[stage.id].orEmpty().count { it in stage.checklist.indices }
    }
    val totalUnits = deliverables.size + totalChecklistItems
    val completedUnits = deliverableScore + completedChecklistItems
    val fraction = if (totalUnits == 0) 0f else (completedUnits / totalUnits).toFloat().coerceIn(0f, 1f)

    return ProgressSummary(
        fraction = fraction,
        percentage = (fraction * 100).roundToInt(),
        approvedDeliverables = deliverables.count { it.status == DeliverableStatus.APPROVED },
        completedChecklistItems = completedChecklistItems,
        totalChecklistItems = totalChecklistItems
    )
}
