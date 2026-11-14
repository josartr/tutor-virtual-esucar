package cl.esucar.tutorvirtual.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import cl.esucar.tutorvirtual.data.local.DATABASE_NAME
import cl.esucar.tutorvirtual.data.local.PasswordHasher
import cl.esucar.tutorvirtual.data.local.TutorDatabaseHelper
import cl.esucar.tutorvirtual.domain.model.AuthResult
import cl.esucar.tutorvirtual.domain.model.Deliverable
import cl.esucar.tutorvirtual.domain.model.DeliverableStatus
import cl.esucar.tutorvirtual.domain.model.InstitutionType
import cl.esucar.tutorvirtual.domain.model.ProjectTopic
import cl.esucar.tutorvirtual.domain.model.UserProfile
import cl.esucar.tutorvirtual.domain.model.UserRole
import cl.esucar.tutorvirtual.domain.model.buildFullName
import cl.esucar.tutorvirtual.domain.model.demoDeliverables
import cl.esucar.tutorvirtual.domain.model.demoStages
import cl.esucar.tutorvirtual.domain.model.isEmailForInstitution
import cl.esucar.tutorvirtual.domain.model.isValidInstitutionalEmail
import cl.esucar.tutorvirtual.domain.repository.AcademicRepository
import cl.esucar.tutorvirtual.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.util.Locale

class SqliteTutorRepository(context: Context) : AuthRepository, AcademicRepository {
    private val appContext = context.applicationContext
    private val database = TutorDatabaseHelper(appContext)
    private val legacyPrefs = appContext.getSharedPreferences("tutor_virtual", Context.MODE_PRIVATE)

    init {
        runCatching {
            database.writableDatabase
            migrateLegacyPreferencesIfNeeded()
        }.onFailure { error ->
            Log.e(TAG, "No se pudo inicializar la base de datos local", error)
            database.close()
            appContext.deleteDatabase(DATABASE_NAME)
            runCatching { database.writableDatabase }
                .onFailure { retryError ->
                    Log.e(TAG, "No se pudo recuperar la base de datos local", retryError)
                }
        }
    }

    private val _currentProfile = MutableStateFlow(loadUserProfile())
    override val currentProfile: StateFlow<UserProfile> = _currentProfile.asStateFlow()

    override fun loadUserProfile(): UserProfile {
        return try {
            database.readableDatabase.rawQuery(
                """
                SELECT u.rank, u.first_name, u.first_last_name, u.second_last_name, u.full_name,
                       u.email, u.research_topic, u.tutor_name, u.sso_provider, u.institution_type, u.user_role
                FROM users u
                INNER JOIN current_session s ON s.user_id = u.id
                WHERE s.id = 1
                """.trimIndent(),
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.toUserProfile(isLoggedIn = true) else UserProfile()
            }
        } catch (error: SQLiteException) {
            Log.e(TAG, "No se pudo cargar la sesión local", error)
            UserProfile()
        }
    }

    override fun saveUserProfile(profile: UserProfile) {
        val resolvedFullName = buildFullName(profile.firstName, profile.firstLastName, profile.secondLastName)
            .ifBlank { profile.fullName.trim() }
        val normalizedProfile = profile.copy(fullName = resolvedFullName)
        val values = ContentValues().apply {
            put("rank", normalizedProfile.rank)
            put("first_name", normalizedProfile.firstName.trim())
            put("first_last_name", normalizedProfile.firstLastName.trim())
            put("second_last_name", normalizedProfile.secondLastName.trim())
            put("full_name", normalizedProfile.fullName)
            put("email", normalizedProfile.email)
            put("research_topic", normalizedProfile.researchTopic)
            put("tutor_name", normalizedProfile.tutorName)
            put("sso_provider", normalizedProfile.ssoProvider)
            put("institution_type", normalizedProfile.institutionType.name)
            put("user_role", normalizedProfile.userRole.name)
        }
        database.writableDatabase.update(
            "users",
            values,
            "id = (SELECT user_id FROM current_session WHERE id = 1)",
            null
        )
        _currentProfile.value = normalizedProfile
    }

    override fun clearSession() {
        database.writableDatabase.delete("current_session", "id = ?", arrayOf("1"))
        _currentProfile.value = UserProfile(isLoggedIn = false)
    }

    override fun registerUser(profile: UserProfile, password: String): AuthResult {
        val email = profile.email.trim().lowercase(Locale.ROOT)
        val resolvedFullName = buildFullName(profile.firstName, profile.firstLastName, profile.secondLastName)
            .ifBlank { profile.fullName.trim() }
        val splitValid = if (
            profile.firstName.isNotBlank() ||
            profile.firstLastName.isNotBlank() ||
            profile.secondLastName.isNotBlank()
        ) {
            profile.firstName.trim().length >= 2 &&
                profile.firstLastName.trim().length >= 2 &&
                profile.secondLastName.trim().length >= 2
        } else {
            resolvedFullName.length >= 3
        }
        if (
            !isValidInstitutionalEmail(email) ||
            !isEmailForInstitution(email, profile.institutionType) ||
            password.length < 8 ||
            !splitValid ||
            profile.researchTopic.trim().length < 4
        ) {
            return AuthResult.InvalidRegistration
        }
        val salt = PasswordHasher.newSalt()
        val values = ContentValues().apply {
            put("email", email)
            put("first_name", profile.firstName.trim())
            put("first_last_name", profile.firstLastName.trim())
            put("second_last_name", profile.secondLastName.trim())
            put("full_name", resolvedFullName)
            put("research_topic", profile.researchTopic.trim())
            put("tutor_name", profile.tutorName.trim())
            put("rank", profile.rank)
            put("institution_type", profile.institutionType.name)
            put("user_role", profile.userRole.name)
            put("password_hash", PasswordHasher.hash(password, salt))
            put("password_salt", salt)
            put("sso_provider", profile.ssoProvider)
        }
        val db = database.writableDatabase
        return try {
            try {
                val userId = db.insertOrThrow("users", null, values)
                setCurrentSession(userId)
                val loggedProfile = profile.copy(fullName = resolvedFullName, email = email, isLoggedIn = true)
                _currentProfile.value = loggedProfile
                AuthResult.Success(loggedProfile)
            } catch (_: SQLiteConstraintException) {
                val legacyUserId = db.query(
                    "users",
                    arrayOf("id", "password_hash"),
                    "email = ?",
                    arrayOf(email),
                    null,
                    null,
                    null
                ).use { cursor ->
                    if (cursor.moveToFirst() && cursor.getString(1).isBlank()) cursor.getLong(0) else null
                }
                if (legacyUserId == null) {
                    AuthResult.EmailAlreadyRegistered
                } else {
                    db.update("users", values, "id = ?", arrayOf(legacyUserId.toString()))
                    setCurrentSession(legacyUserId)
                    val loggedProfile = profile.copy(fullName = resolvedFullName, email = email, isLoggedIn = true)
                    _currentProfile.value = loggedProfile
                    AuthResult.Success(loggedProfile)
                }
            }
        } catch (_: SQLiteException) {
            AuthResult.PersistenceError
        }
    }

    override fun login(emailInput: String, password: String): AuthResult {
        val email = emailInput.trim().lowercase(Locale.ROOT)
        return try {
            database.readableDatabase.query(
                "users",
                USER_COLUMNS,
                "email = ?",
                arrayOf(email),
                null,
                null,
                null
            ).use { cursor ->
                if (!cursor.moveToFirst()) return AuthResult.InvalidCredentials
                val salt = cursor.getString(cursor.getColumnIndexOrThrow("password_salt"))
                val expectedHash = cursor.getString(cursor.getColumnIndexOrThrow("password_hash"))
                if (!PasswordHasher.verify(password, salt, expectedHash)) {
                    return AuthResult.InvalidCredentials
                }
                val userId = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                setCurrentSession(userId)
                val loggedProfile = cursor.toUserProfile(isLoggedIn = true)
                _currentProfile.value = loggedProfile
                AuthResult.Success(loggedProfile)
            }
        } catch (_: SQLiteException) {
            AuthResult.PersistenceError
        }
    }

    override fun loginQuickSso(provider: String): UserProfile {
        val demoProfile = UserProfile(
            rank = "Sargento 2°",
            firstName = "Marcos",
            firstLastName = "Soto",
            secondLastName = "Rojas",
            fullName = "Marcos Soto Rojas",
            email = "marcos.soto@carabineros.cl",
            researchTopic = "Estrategias de Seguridad Preventiva Cuadrante",
            tutorName = "Mayor R. González",
            ssoProvider = provider,
            institutionType = InstitutionType.CARABINEROS,
            userRole = UserRole.FUNCIONARIO,
            isLoggedIn = true
        )
        _currentProfile.value = demoProfile
        saveUserProfile(demoProfile)
        return demoProfile
    }

    private fun setCurrentSession(userId: Long) {
        database.writableDatabase.insertWithOnConflict(
            "current_session",
            null,
            ContentValues().apply {
                put("id", 1)
                put("user_id", userId)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    override fun loadDeliverables(): List<Deliverable> {
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

    override fun saveDeliverables(items: List<Deliverable>) {
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

    override fun loadChecklist(): Map<String, Set<Int>> {
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

    override fun saveChecklist(checklist: Map<String, Set<Int>>) {
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

    override fun loadStageNotes(): Map<String, String> {
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

    override fun saveStageNote(stageId: String, note: String) {
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

    override fun loadProjectTopics(): List<ProjectTopic> {
        return try {
            database.readableDatabase.query(
                "project_topics",
                PROJECT_TOPIC_COLUMNS,
                null,
                null,
                null,
                null,
                "rowid DESC"
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.toProjectTopic())
                }
            }
        } catch (_: SQLiteException) {
            emptyList()
        }
    }

    override fun addProjectTopic(topic: ProjectTopic) {
        database.writableDatabase.insertWithOnConflict(
            "project_topics",
            null,
            ContentValues().apply {
                put("id", topic.id)
                put("title", topic.title.trim())
                put("created_at", topic.createdAt)
            },
            SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    override fun deleteProjectTopic(id: String) {
        database.writableDatabase.delete("project_topics", "id = ?", arrayOf(id))
    }

    private fun migrateLegacyPreferencesIfNeeded() {
        val db = database.writableDatabase
        migrateLegacyDatabaseProfile(db)
        val hasLegacyData = legacyPrefs.all.isNotEmpty()
        if (!hasLegacyData) return

        val hasUsers = db.query("users", arrayOf("id"), null, null, null, null, null)
            .use { it.moveToFirst() }
        if (!hasUsers) {
            val legacyProfile = UserProfile(
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
            if (legacyProfile.email.isNotBlank()) {
                insertLegacyProfile(db, legacyProfile)
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

    private fun migrateLegacyDatabaseProfile(db: SQLiteDatabase) {
        val hasUsers = db.query("users", arrayOf("id"), null, null, null, null, null)
            .use { it.moveToFirst() }
        if (hasUsers) return

        db.query(
            "user_profile",
            arrayOf(
                "rank", "full_name", "email", "research_topic", "tutor_name",
                "sso_provider", "is_logged_in"
            ),
            null,
            null,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val profile = UserProfile(
                    rank = cursor.getString(0),
                    fullName = cursor.getString(1),
                    email = cursor.getString(2),
                    researchTopic = cursor.getString(3),
                    tutorName = cursor.getString(4),
                    ssoProvider = cursor.getString(5),
                    isLoggedIn = cursor.getInt(6) != 0
                )
                if (profile.email.isNotBlank()) {
                    insertLegacyProfile(db, profile)
                }
            }
        }
    }

    private fun insertLegacyProfile(db: SQLiteDatabase, profile: UserProfile) {
        val userId = db.insertWithOnConflict(
            "users",
            null,
            ContentValues().apply {
                put("email", profile.email.trim().lowercase(Locale.ROOT))
                put("full_name", profile.fullName)
                put("research_topic", profile.researchTopic)
                put("tutor_name", profile.tutorName)
                put("rank", profile.rank)
                put("institution_type", profile.institutionType.name)
                put("user_role", profile.userRole.name)
                put("password_hash", "")
                put("password_salt", "")
                put("sso_provider", profile.ssoProvider)
            },
            SQLiteDatabase.CONFLICT_IGNORE
        )
        if (userId != -1L && profile.isLoggedIn) setCurrentSession(userId)
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

    private fun Cursor.toUserProfile(isLoggedIn: Boolean = false): UserProfile {
        val fName = getColumnIndex("first_name").takeIf { it >= 0 }?.let { getString(it) }.orEmpty()
        val fLast = getColumnIndex("first_last_name").takeIf { it >= 0 }?.let { getString(it) }.orEmpty()
        val sLast = getColumnIndex("second_last_name").takeIf { it >= 0 }?.let { getString(it) }.orEmpty()
        val legacyFull = getString(getColumnIndexOrThrow("full_name"))
        val computedFull = buildFullName(fName, fLast, sLast).ifBlank { legacyFull }
        return UserProfile(
            rank = getString(getColumnIndexOrThrow("rank")),
            firstName = fName,
            firstLastName = fLast,
            secondLastName = sLast,
            fullName = computedFull,
            email = getString(getColumnIndexOrThrow("email")),
            researchTopic = getString(getColumnIndexOrThrow("research_topic")),
            tutorName = getString(getColumnIndexOrThrow("tutor_name")),
            ssoProvider = getString(getColumnIndexOrThrow("sso_provider")),
            institutionType = runCatching {
                InstitutionType.valueOf(getString(getColumnIndexOrThrow("institution_type")))
            }.getOrDefault(InstitutionType.ESUCAR),
            userRole = getColumnIndex("user_role").takeIf { it >= 0 }?.let { idx ->
                runCatching { UserRole.valueOf(getString(idx)) }.getOrNull()
            } ?: UserRole.ESTUDIANTE,
            isLoggedIn = isLoggedIn
        )
    }

    private fun Cursor.toProjectTopic() = ProjectTopic(
        id = getString(getColumnIndexOrThrow("id")),
        title = getString(getColumnIndexOrThrow("title")),
        createdAt = getString(getColumnIndexOrThrow("created_at"))
    )

    private fun Cursor.toDeliverable() = Deliverable(
        id = getString(getColumnIndexOrThrow("id")),
        title = getString(getColumnIndexOrThrow("title")),
        dueDate = getString(getColumnIndexOrThrow("due_date")),
        status = DeliverableStatus.valueOf(getString(getColumnIndexOrThrow("status"))),
        stageId = getString(getColumnIndexOrThrow("stage_id"))
    )

    companion object {
        private const val TAG = "SqliteTutorRepository"
        private val USER_COLUMNS = arrayOf(
            "id", "rank", "first_name", "first_last_name", "second_last_name", "full_name",
            "email", "research_topic", "tutor_name", "sso_provider", "institution_type",
            "user_role", "password_hash", "password_salt"
        )
        private val PROJECT_TOPIC_COLUMNS = arrayOf("id", "title", "created_at")
        private val DELIVERABLE_COLUMNS = arrayOf("id", "title", "due_date", "status", "stage_id")
    }
}
