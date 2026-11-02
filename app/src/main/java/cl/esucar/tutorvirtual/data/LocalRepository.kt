package cl.esucar.tutorvirtual.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LocalRepository(context: Context) {
    private val prefs = context.getSharedPreferences("tutor_virtual", Context.MODE_PRIVATE)

    fun loadDeliverables(): List<Deliverable> {
        val raw = prefs.getString("deliverables", null) ?: return demoDeliverables
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                Deliverable(item.getString("id"), item.getString("title"), item.getString("dueDate"),
                    DeliverableStatus.valueOf(item.getString("status")), item.getString("stageId"))
            }
        }.getOrDefault(demoDeliverables)
    }

    fun saveDeliverables(items: List<Deliverable>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id); put("title", item.title); put("dueDate", item.dueDate)
                put("status", item.status.name); put("stageId", item.stageId)
            })
        }
        prefs.edit().putString("deliverables", array.toString()).apply()
    }
}
