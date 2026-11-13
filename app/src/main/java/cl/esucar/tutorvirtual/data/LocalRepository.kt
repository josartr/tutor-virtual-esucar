package cl.esucar.tutorvirtual.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray

private const val DATABASE_NAME = "tutor_virtual.db"
private const val DATABASE_VERSION = 1

private class TutorDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE user_profile (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                rank TEXT NOT NULL,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL,
                research_topic TEXT NOT NULL,
                tutor_name TEXT NOT NULL,
                sso_provider TEXT NOT NULL,
                is_logged_in INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE deliverables (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                due_date TEXT NOT NULL,
                status TEXT NOT NULL,
                stage_id TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE checklist_items (
                stage_id TEXT NOT NULL,
                item_index INTEGER NOT NULL,
                PRIMARY KEY (stage_id, item_index)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE stage_notes (
                stage_id TEXT PRIMARY KEY NOT NULL,
                note TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Keep schema changes explicit in future versions instead of dropping user data.
    }
}

class LocalRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = TutorDatabaseHelper(appContext)
    private val legacyPrefs = appContext.getSharedPreferences("tutor_virtual", Context.MODE_PRIVATE)

    init {
        migrateLegacyPreferencesIfNeeded()
    }

    fun loadUserProfile(): UserProfile {
        database.readableDatabase.query(
            "user_profile",
            PROFILE_COLUMNS,
            "id = ?",
            arrayOf("1"),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.toUserProfile()
            }
        }
        return UserProfile()
    }

    fun saveUserProfile(profile: UserProfile) {
        val values = ContentValues().apply {
            put("id", 1)
            put("rank", profile.rank)
            put("full_name", profile.fullName)
            put("email", profile.email)
            put("research_topic", profile.researchTopic)
            put("tutor_name", profile.tutorName)
            put("sso_provider", profile.ssoProvider)
            put("is_logged_in", if (profile.isLoggedIn) 1 else 0)
        }
        database.writableDatabase.insertWithOnConflict(
            "user_profile",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun clearUserProfile() {
        database.writableDatabase.delete("user_profile", "id = ?", arrayOf("1"))
    }

    fun loadDeliverables(): List<Deliverable> {
        database.readableDatabase.query(
            "deliverables",
            DELIVERABLE_COLUMNS,
            null,
            null,
            null,
            null,
            "rowid ASC"
        ).use { cursor ->
            if (cursor.count == 0) return demoDeliverables
            return buildList {
                while (cursor.moveToNext()) add(cursor.toDeliverable())
            }
        }
    }

    fun saveDeliverables(items: List<Deliverable>) {
        val db = database.writableDatabase
        db.transaction {
            db.delete("deliverables", null, null)
            items.forEach { item ->
                db.insertOrThrow("deliverables", null, ContentValues().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("due_date", item.dueDate)
                    put("status", item.status.name)
                    put("stage_id", item.stageId)
                })
            }
        }
    }

    fun loadChecklist(): Map<String, Set<Int>> {
        val loaded = demoStages.associate { it.id to mutableSetOf<Int>() }.toMutableMap()
        database.readableDatabase.query(
            "checklist_items",
            arrayOf("stage_id", "item_index"),
            null,
            null,
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                loaded.getOrPut(cursor.getString(0)) { mutableSetOf() }.add(cursor.getInt(1))
            }
        }
        return loaded.mapValues { it.value.toSet() }
    }

    fun saveChecklist(checklist: Map<String, Set<Int>>) {
        val db = database.writableDatabase
        db.transaction {
            db.delete("checklist_items", null, null)
            checklist.forEach { (stageId, completed) ->
                completed.forEach { index ->
                    db.insertOrThrow("checklist_items", null, ContentValues().apply {
                        put("stage_id", stageId)
                        put("item_index", index)
                    })
                }
            }
        }
    }

    fun loadStageNotes(): Map<String, String> {
        val notes = demoStages.associate { it.id to "" }.toMutableMap()
        database.readableDatabase.query(
            "stage_notes",
            arrayOf("stage_id", "note"),
            null,
            null,
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) notes[cursor.getString(0)] = cursor.getString(1)
        }
        return notes
    }

    fun saveStageNote(stageId: String, note: String) {
        database.writableDatabase.insertWithOnConflict(
            "stage_notes",
            null,
            ContentValues().apply {
                put("stage_id", stageId)
                put("note", note)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun migrateLegacyPreferencesIfNeeded() {
        val db = database.writableDatabase
        val hasLegacyData = legacyPrefs.all.isNotEmpty()
        if (!hasLegacyData) return

        db.query("user_profile", arrayOf("id"), "id = ?", arrayOf("1"), null, null, null).use { cursor ->
            if (!cursor.moveToFirst()) {
                saveUserProfile(
                    UserProfile(
                        rank = legacyPrefs.getString("user_rank", "Alumno ESUCAR") ?: "Alumno ESUCAR",
                        fullName = legacyPrefs.getString("user_full_name", "") ?: "",
                        email = legacyPrefs.getString("user_email", "") ?: "",
                        researchTopic = legacyPrefs.getString("user_research_topic", "") ?: "",
                        tutorName = legacyPrefs.getString("user_tutor_name", "") ?: "",
                        ssoProvider = legacyPrefs.getString(
                            "user_sso_provider",
                            "Correo Institucional"
                        ) ?: "Correo Institucional",
                        isLoggedIn = legacyPrefs.getBoolean("user_is_logged_in", false)
                    )
                )
            }
        }

        val hasDeliverables = db.query("deliverables", arrayOf("id"), null, null, null, null, null)
            .use { it.moveToFirst() }
        if (!hasDeliverables) legacyPrefs.getString("deliverables", null)?.let { raw ->
            runCatching {
                val items = JSONArray(raw).let { array ->
                    List(array.length()) { index ->
                        array.getJSONObject(index).let { item ->
                            Deliverable(
                                item.getString("id"),
                                item.getString("title"),
                                item.getString("dueDate"),
                                DeliverableStatus.valueOf(item.getString("status")),
                                item.optString("stageId", "planteamiento")
                            )
                        }
                    }
                }
                saveDeliverables(items)
            }
        }

        val hasChecklist = db.query(
            "checklist_items",
            arrayOf("stage_id"),
            null,
            null,
            null,
            null,
            null
        ).use { it.moveToFirst() }
        val hasNotes = db.query("stage_notes", arrayOf("stage_id"), null, null, null, null, null)
            .use { it.moveToFirst() }

        demoStages.forEach { stage ->
            if (!hasChecklist) {
                legacyPrefs.getStringSet("checklist_${stage.id}", null)?.let { completed ->
                    val dbValues = completed.mapNotNull(String::toIntOrNull).map { index ->
                        ContentValues().apply {
                            put("stage_id", stage.id)
                            put("item_index", index)
                        }
                    }
                    dbValues.forEach { values ->
                        db.insertWithOnConflict(
                            "checklist_items",
                            null,
                            values,
                            SQLiteDatabase.CONFLICT_IGNORE
                        )
                    }
                }
            }
            if (!hasNotes) {
                legacyPrefs.getString("note_${stage.id}", null)?.let { note ->
                    db.insertWithOnConflict(
                        "stage_notes",
                        null,
                        ContentValues().apply {
                            put("stage_id", stage.id)
                            put("note", note)
                        },
                        SQLiteDatabase.CONFLICT_REPLACE
                    )
                }
            }
        }
    }

    private fun SQLiteDatabase.transaction(block: () -> Unit) {
        beginTransaction()
        try {
            block()
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    private fun android.database.Cursor.toUserProfile() = UserProfile(
        rank = getString(getColumnIndexOrThrow("rank")),
        fullName = getString(getColumnIndexOrThrow("full_name")),
        email = getString(getColumnIndexOrThrow("email")),
        researchTopic = getString(getColumnIndexOrThrow("research_topic")),
        tutorName = getString(getColumnIndexOrThrow("tutor_name")),
        ssoProvider = getString(getColumnIndexOrThrow("sso_provider")),
        isLoggedIn = getInt(getColumnIndexOrThrow("is_logged_in")) != 0
    )

    private fun android.database.Cursor.toDeliverable() = Deliverable(
        id = getString(getColumnIndexOrThrow("id")),
        title = getString(getColumnIndexOrThrow("title")),
        dueDate = getString(getColumnIndexOrThrow("due_date")),
        status = DeliverableStatus.valueOf(getString(getColumnIndexOrThrow("status"))),
        stageId = getString(getColumnIndexOrThrow("stage_id"))
    )

    companion object {
        private val PROFILE_COLUMNS = arrayOf(
            "rank", "full_name", "email", "research_topic", "tutor_name", "sso_provider",
            "is_logged_in"
        )
        private val DELIVERABLE_COLUMNS = arrayOf("id", "title", "due_date", "status", "stage_id")
    }
}
