package cl.esucar.tutorvirtual.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Base64
import org.json.JSONArray
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private const val DATABASE_NAME = "tutor_virtual.db"
private const val DATABASE_VERSION = 2

sealed interface AuthResult {
    data class Success(val profile: UserProfile) : AuthResult
    data object EmailAlreadyRegistered : AuthResult
    data object InvalidCredentials : AuthResult
    data object InvalidRegistration : AuthResult
}

private object PasswordHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    fun newSalt(): String {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hash(password: String, salt: String): String {
        val spec = PBEKeySpec(
            password.toCharArray(),
            Base64.decode(salt, Base64.DEFAULT),
            ITERATIONS,
            KEY_LENGTH
        )
        return try {
            Base64.encodeToString(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .encoded,
                Base64.NO_WRAP
            )
        } finally {
            spec.clearPassword()
        }
    }
}

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
                institution_type TEXT NOT NULL DEFAULT 'ESUCAR',
                password_hash TEXT NOT NULL DEFAULT '',
                password_salt TEXT NOT NULL DEFAULT '',
                is_logged_in INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT NOT NULL UNIQUE COLLATE NOCASE,
                full_name TEXT NOT NULL,
                research_topic TEXT NOT NULL,
                tutor_name TEXT NOT NULL,
                rank TEXT NOT NULL,
                institution_type TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                password_salt TEXT NOT NULL,
                sso_provider TEXT NOT NULL
            )
            """.trimIndent()
        )
        createAuthTables(db)
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
        if (oldVersion < 2) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    email TEXT NOT NULL UNIQUE COLLATE NOCASE,
                    full_name TEXT NOT NULL,
                    research_topic TEXT NOT NULL,
                    tutor_name TEXT NOT NULL,
                    rank TEXT NOT NULL,
                    institution_type TEXT NOT NULL DEFAULT 'ESUCAR',
                    password_hash TEXT NOT NULL DEFAULT '',
                    password_salt TEXT NOT NULL DEFAULT '',
                    sso_provider TEXT NOT NULL DEFAULT 'Correo Institucional'
                )
                """.trimIndent()
            )
            createAuthTables(db)
        }
    }

    private fun createAuthTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS current_session (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                user_id INTEGER NOT NULL REFERENCES users(id)
            )
            """.trimIndent()
        )
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
            "users u INNER JOIN current_session s ON s.user_id = u.id",
            PROFILE_COLUMNS,
            "s.id = ?",
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
            put("rank", profile.rank)
            put("full_name", profile.fullName)
            put("email", profile.email)
            put("research_topic", profile.researchTopic)
            put("tutor_name", profile.tutorName)
            put("sso_provider", profile.ssoProvider)
            put("institution_type", profile.institutionType.name)
        }
        database.writableDatabase.update(
            "users",
            values,
            "id = (SELECT user_id FROM current_session WHERE id = 1)",
            null
        )
    }

    fun clearUserProfile() {
        database.writableDatabase.delete("current_session", "id = ?", arrayOf("1"))
    }

    fun registerUser(profile: UserProfile, password: String): AuthResult {
        val email = profile.email.trim().lowercase()
        if (
            !isValidInstitutionalEmail(email) ||
            !isEmailForInstitution(email, profile.institutionType) ||
            password.length < 8 ||
            profile.fullName.trim().length < 3 ||
            profile.researchTopic.trim().length < 4
        ) {
            return AuthResult.InvalidRegistration
        }
        val salt = PasswordHasher.newSalt()
        val values = ContentValues().apply {
            put("email", email)
            put("full_name", profile.fullName.trim())
            put("research_topic", profile.researchTopic.trim())
            put("tutor_name", profile.tutorName.trim())
            put("rank", profile.rank)
            put("institution_type", profile.institutionType.name)
            put("password_hash", PasswordHasher.hash(password, salt))
            put("password_salt", salt)
            put("sso_provider", profile.ssoProvider)
        }
        val db = database.writableDatabase
        return try {
            val userId = db.insertOrThrow("users", null, values)
            setCurrentSession(userId)
            AuthResult.Success(profile.copy(email = email, isLoggedIn = true))
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
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
                AuthResult.Success(profile.copy(email = email, isLoggedIn = true))
            }
        }
    }

    fun login(emailInput: String, password: String): AuthResult {
        val email = emailInput.trim().lowercase()
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
            if (salt.isBlank() || !MessageDigest.isEqual(
                    expectedHash.toByteArray(),
                    PasswordHasher.hash(password, salt).toByteArray()
                )
            ) {
                return AuthResult.InvalidCredentials
            }
            val userId = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
            setCurrentSession(userId)
            return AuthResult.Success(cursor.toUserProfile(isLoggedIn = true))
        }
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
                put("email", profile.email.trim().lowercase())
                put("full_name", profile.fullName)
                put("research_topic", profile.researchTopic)
                put("tutor_name", profile.tutorName)
                put("rank", profile.rank)
                put("institution_type", profile.institutionType.name)
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

    private fun android.database.Cursor.toUserProfile(isLoggedIn: Boolean = false) = UserProfile(
        rank = getString(getColumnIndexOrThrow("rank")),
        fullName = getString(getColumnIndexOrThrow("full_name")),
        email = getString(getColumnIndexOrThrow("email")),
        researchTopic = getString(getColumnIndexOrThrow("research_topic")),
        tutorName = getString(getColumnIndexOrThrow("tutor_name")),
        ssoProvider = getString(getColumnIndexOrThrow("sso_provider")),
        institutionType = runCatching {
            InstitutionType.valueOf(getString(getColumnIndexOrThrow("institution_type")))
        }.getOrDefault(InstitutionType.ESUCAR),
        isLoggedIn = isLoggedIn
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
            "u.rank AS rank", "u.full_name AS full_name", "u.email AS email",
            "u.research_topic AS research_topic", "u.tutor_name AS tutor_name",
            "u.sso_provider AS sso_provider", "u.institution_type AS institution_type"
        )
        private val USER_COLUMNS = arrayOf(
            "id", "rank", "full_name", "email", "research_topic", "tutor_name",
            "sso_provider", "institution_type"
        )
        private val DELIVERABLE_COLUMNS = arrayOf("id", "title", "due_date", "status", "stage_id")
    }
}
