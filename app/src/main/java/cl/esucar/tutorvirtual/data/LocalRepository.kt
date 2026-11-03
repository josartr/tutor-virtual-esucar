package cl.esucar.tutorvirtual.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LocalRepository(context: Context) {
    private val prefs = context.getSharedPreferences("tutor_virtual", Context.MODE_PRIVATE)

    fun loadUserProfile(): UserProfile {
        val isLoggedIn = prefs.getBoolean("user_is_logged_in", false)
        val rank = prefs.getString("user_rank", "Alumno ESUCAR") ?: "Alumno ESUCAR"
        val fullName = prefs.getString("user_full_name", "") ?: ""
        val email = prefs.getString("user_email", "") ?: ""
        val researchTopic = prefs.getString("user_research_topic", "") ?: ""
        val tutorName = prefs.getString("user_tutor_name", "") ?: ""
        val ssoProvider = prefs.getString("user_sso_provider", "Carabineros SSO") ?: "Carabineros SSO"

        return UserProfile(
            rank = rank,
            fullName = fullName,
            email = email,
            researchTopic = researchTopic,
            tutorName = tutorName,
            ssoProvider = ssoProvider,
            isLoggedIn = isLoggedIn
        )
    }

    fun saveUserProfile(profile: UserProfile) {
        prefs.edit()
            .putBoolean("user_is_logged_in", profile.isLoggedIn)
            .putString("user_rank", profile.rank)
            .putString("user_full_name", profile.fullName)
            .putString("user_email", profile.email)
            .putString("user_research_topic", profile.researchTopic)
            .putString("user_tutor_name", profile.tutorName)
            .putString("user_sso_provider", profile.ssoProvider)
            .apply()
    }

    fun clearUserProfile() {
        prefs.edit()
            .remove("user_is_logged_in")
            .remove("user_rank")
            .remove("user_full_name")
            .remove("user_email")
            .remove("user_research_topic")
            .remove("user_tutor_name")
            .remove("user_sso_provider")
            .apply()
    }

    fun loadDeliverables(): List<Deliverable> {
        val raw = prefs.getString("deliverables", null) ?: return demoDeliverables
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                Deliverable(
                    item.getString("id"),
                    item.getString("title"),
                    item.getString("dueDate"),
                    DeliverableStatus.valueOf(item.getString("status")),
                    item.optString("stageId", "planteamiento")
                )
            }
        }.getOrDefault(demoDeliverables)
    }

    fun saveDeliverables(items: List<Deliverable>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("dueDate", item.dueDate)
                put("status", item.status.name)
                put("stageId", item.stageId)
            })
        }
        prefs.edit().putString("deliverables", array.toString()).apply()
    }

    fun loadChecklist(): Map<String, Set<Int>> = demoStages.associate { stage ->
        stage.id to prefs.getStringSet("checklist_${stage.id}", emptySet())
            .orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .toSet()
    }

    fun saveChecklist(checklist: Map<String, Set<Int>>) {
        val editor = prefs.edit()
        checklist.forEach { (stageId, completed) ->
            editor.putStringSet("checklist_$stageId", completed.map(Int::toString).toSet())
        }
        editor.apply()
    }

    fun loadStageNotes(): Map<String, String> = demoStages.associate { stage ->
        stage.id to (prefs.getString("note_${stage.id}", "") ?: "")
    }

    fun saveStageNote(stageId: String, note: String) {
        prefs.edit().putString("note_$stageId", note).apply()
    }
}
